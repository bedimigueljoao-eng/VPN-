package com.example.data.repository

import com.example.data.local.CarrierUsageSummary
import com.example.data.local.ExportedServerProfile
import com.example.data.local.ServerProfileEntity
import com.example.data.local.UsageSessionEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VaultConfigBundle
import com.example.data.local.VaultDao
import com.example.data.local.VaultPreferences
import com.example.model.CarrierSelectionMode
import com.example.ui.theme.ThemeMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

class VaultRepository(
    private val dao: VaultDao,
    private val prefsRepo: UserPreferencesRepository
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val serversFlow: Flow<List<ServerProfileEntity>> = dao.observeServers()
    val selectedServerFlow: Flow<ServerProfileEntity?> = dao.observeSelectedServer()
    val usageSessionsFlow: Flow<List<UsageSessionEntity>> = dao.observeUsageSessions()
    val carrierSummariesFlow: Flow<List<CarrierUsageSummary>> = dao.observeCarrierUsageSummaries()
    val preferencesFlow: Flow<VaultPreferences> = prefsRepo.preferencesFlow

    suspend fun getSelectedServerOnce(): ServerProfileEntity? = dao.getSelectedServerOnce()

    suspend fun getServerById(id: Long): ServerProfileEntity? = dao.getServerById(id)

    suspend fun selectServer(id: Long) = dao.selectServer(id)

    suspend fun saveServer(server: ServerProfileEntity): Long {
        val insertedId = if (server.id == 0L) {
            dao.insertServer(server)
        } else {
            dao.updateServer(server)
            server.id
        }
        if (server.isSelected) {
            dao.selectServer(insertedId)
        }
        return insertedId
    }

    suspend fun deleteServer(id: Long) {
        val wasSelected = dao.getServerById(id)?.isSelected == true
        dao.deleteServerById(id)
        if (wasSelected) {
            dao.getAllServersOnce().firstOrNull()?.let { fallback ->
                dao.selectServer(fallback.id)
            }
        }
    }

    suspend fun markServerPreparedOffline(
        id: Long,
        prepared: Boolean,
        timestamp: Long,
        latencyMs: Long?
    ) = dao.updateServerVaultStatus(id, prepared, timestamp, latencyMs)

    suspend fun updateServerLatency(id: Long, latencyMs: Long?) =
        dao.updateServerLatency(id, latencyMs)

    suspend fun startSession(
        server: ServerProfileEntity,
        carrierType: String,
        transportType: String,
        vpnIp: String
    ): Long {
        return dao.insertSession(
            UsageSessionEntity(
                serverName = server.name,
                serverHost = server.host,
                carrierType = carrierType,
                transportType = transportType,
                protocol = server.protocol,
                vpnIp = vpnIp,
                startedAt = System.currentTimeMillis(),
                status = "ACTIVA"
            )
        )
    }

    suspend fun finishSession(
        sessionId: Long,
        durationSeconds: Long,
        rxBytes: Long,
        txBytes: Long,
        status: String
    ) {
        dao.finishSession(
            sessionId = sessionId,
            endedAt = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            rxBytes = rxBytes,
            txBytes = txBytes,
            status = status
        )
    }

    suspend fun clearUsageHistory() = dao.clearUsageHistory()

    suspend fun setCarrierSelectionMode(mode: CarrierSelectionMode) =
        prefsRepo.setCarrierSelectionMode(mode)

    suspend fun setThemeMode(mode: ThemeMode) = prefsRepo.setThemeMode(mode)
    suspend fun setAutoPrepareOnWifi(enabled: Boolean) = prefsRepo.setAutoPrepareOnWifi(enabled)
    suspend fun setDnsLeakGuard(enabled: Boolean) = prefsRepo.setDnsLeakGuard(enabled)
    suspend fun setBypassLan(enabled: Boolean) = prefsRepo.setBypassLan(enabled)
    suspend fun updateLastWifiPrep(timestamp: Long) = prefsRepo.updateLastWifiPrep(timestamp)

    suspend fun exportServersToJson(): String {
        val servers = dao.getAllServersOnce()
        val bundle = VaultConfigBundle(
            exportedAtMillis = System.currentTimeMillis(),
            profiles = servers.map {
                ExportedServerProfile(
                    name = it.name,
                    host = it.host,
                    port = it.port,
                    protocol = it.protocol,
                    tunnelMode = it.tunnelMode,
                    dnsPrimary = it.dnsPrimary,
                    dnsSecondary = it.dnsSecondary,
                    mtu = it.mtu,
                    preferredCarrier = it.preferredCarrier,
                    authUsername = it.authUsername,
                    publicKeyOrToken = it.publicKeyOrToken,
                    notes = it.notes
                )
            }
        )
        return moshi.adapter(VaultConfigBundle::class.java).indent("  ").toJson(bundle)
    }

    suspend fun importServersFromJson(json: String): Int {
        val adapter = moshi.adapter(VaultConfigBundle::class.java)
        val bundle = adapter.fromJson(json) ?: return 0
        val now = System.currentTimeMillis()
        var count = 0
        bundle.profiles.forEach { profile ->
            if (profile.host.isNotBlank() && profile.port in 1..65535) {
                dao.insertServer(
                    ServerProfileEntity(
                        name = profile.name.ifBlank { "Servidor Importado" },
                        host = profile.host.trim(),
                        port = profile.port,
                        protocol = profile.protocol,
                        tunnelMode = profile.tunnelMode,
                        dnsPrimary = profile.dnsPrimary.ifBlank { "1.1.1.1" },
                        dnsSecondary = profile.dnsSecondary.ifBlank { "8.8.8.8" },
                        mtu = profile.mtu.coerceIn(1280, 1500),
                        preferredCarrier = profile.preferredCarrier.ifBlank { "ALL" },
                        authUsername = profile.authUsername,
                        publicKeyOrToken = profile.publicKeyOrToken,
                        isPreparedOffline = true,
                        lastPreparedAt = now,
                        isSelected = false,
                        isCustom = true,
                        notes = profile.notes
                    )
                )
                count++
            }
        }
        return count
    }
}

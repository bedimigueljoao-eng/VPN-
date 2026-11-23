package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.TunnelMode
import com.example.model.VpnProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ServerProfileEntity::class,
        UsageSessionEntity::class,
        ConnectivityLabReportEntity::class,
        DocumentedServiceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `connectivity_lab_reports` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `carrierTested` TEXT NOT NULL,
                        `detectedMccMnc` TEXT NOT NULL,
                        `transportType` TEXT NOT NULL,
                        `balanceState` TEXT NOT NULL,
                        `classification` TEXT NOT NULL,
                        `connectivitySummaryPt` TEXT NOT NULL,
                        `destinationsJson` TEXT NOT NULL,
                        `vpnActive` INTEGER NOT NULL,
                        `vpnServerName` TEXT NOT NULL,
                        `vpnIp` TEXT NOT NULL,
                        `publicIpBefore` TEXT NOT NULL,
                        `publicIpAfter` TEXT NOT NULL,
                        `dataConsumedBytes` INTEGER NOT NULL,
                        `technicalConclusionPt` TEXT NOT NULL,
                        `executedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `documented_services` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `carrierType` TEXT NOT NULL,
                        `serviceName` TEXT NOT NULL,
                        `domainOrHost` TEXT NOT NULL,
                        `port` INTEGER NOT NULL,
                        `sourceReference` TEXT NOT NULL,
                        `verifiedDate` TEXT NOT NULL,
                        `conditionsPt` TEXT NOT NULL,
                        `validityNotePt` TEXT NOT NULL,
                        `isOfficiallyAuthorized` INTEGER NOT NULL,
                        `isUserAdded` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                seedDefaultDocumentedServicesSql(db)
            }
        }

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): VaultDatabase {
            return Room.databaseBuilder(
                appContext,
                VaultDatabase::class.java,
                "internet_vault.db"
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = INSTANCE?.vaultDao()
                            dao?.insertServers(defaultServers())
                            dao?.insertDocumentedServices(defaultDocumentedServices())
                        }
                    }
                })
                .build()
        }

        private fun seedDefaultDocumentedServicesSql(db: SupportSQLiteDatabase) {
            defaultDocumentedServices().forEach { s ->
                db.execSQL(
                    """
                    INSERT INTO documented_services (
                        carrierType, serviceName, domainOrHost, port, sourceReference,
                        verifiedDate, conditionsPt, validityNotePt, isOfficiallyAuthorized, isUserAdded
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(
                        s.carrierType,
                        s.serviceName,
                        s.domainOrHost,
                        s.port,
                        s.sourceReference,
                        s.verifiedDate,
                        s.conditionsPt,
                        s.validityNotePt,
                        if (s.isOfficiallyAuthorized) 1 else 0,
                        if (s.isUserAdded) 1 else 0
                    )
                )
            }
        }

        private fun defaultDocumentedServices(): List<DocumentedServiceEntity> = listOf(
            DocumentedServiceEntity(
                carrierType = "UNITEL",
                serviceName = "Portal Oficial Unitel / My Unitel",
                domainOrHost = "unitel.ao",
                port = 443,
                sourceReference = "Documentação pública oficial unitel.ao / Google Play (My Unitel)",
                verifiedDate = "23/11/2026",
                conditionsPt = "Canal oficial de autoatendimento para gestão de conta, saldos e compra de pacotes. Não garante trânsito aberto para servidores externos.",
                validityNotePt = "Sujeito às regras comerciais vigentes da Unitel Angola.",
                isOfficiallyAuthorized = true,
                isUserAdded = false
            ),
            DocumentedServiceEntity(
                carrierType = "AFRICELL",
                serviceName = "Portal Oficial Africell / MyAfricell AO",
                domainOrHost = "africell.ao",
                port = 443,
                sourceReference = "Documentação pública oficial africell.ao / Google Play (MyAfricell AO)",
                verifiedDate = "23/11/2026",
                conditionsPt = "Balcão digital oficial para consulta de saldos, compra de pacotes Afrinet e serviços Afrimoney. Não permite encaminhamento VPN geral.",
                validityNotePt = "Sujeito às políticas comerciais vigentes da Africell Angola.",
                isOfficiallyAuthorized = true,
                isUserAdded = false
            )
        )

        private fun defaultServers(): List<ServerProfileEntity> {
            val now = System.currentTimeMillis()
            return listOf(
                ServerProfileEntity(
                    name = "Cofre DNS Seguro · Cloudflare",
                    host = "1.1.1.1",
                    port = 443,
                    protocol = VpnProtocol.DNS_VAULT_GUARD.name,
                    tunnelMode = TunnelMode.DNS_VAULT_SPLIT.name,
                    dnsPrimary = "1.1.1.1",
                    dnsSecondary = "1.0.0.1",
                    mtu = 1420,
                    preferredCarrier = "ALL",
                    isPreparedOffline = true,
                    lastPreparedAt = now,
                    isSelected = true,
                    isCustom = false,
                    notes = "Perfil pronto para testes imediatos. Activa interface TUN segura com reencaminhamento DNS protegido."
                ),
                ServerProfileEntity(
                    name = "Cofre DNS · Google Public",
                    host = "8.8.8.8",
                    port = 443,
                    protocol = VpnProtocol.DNS_VAULT_GUARD.name,
                    tunnelMode = TunnelMode.DNS_VAULT_SPLIT.name,
                    dnsPrimary = "8.8.8.8",
                    dnsSecondary = "8.8.4.4",
                    mtu = 1420,
                    preferredCarrier = "UNITEL",
                    isPreparedOffline = true,
                    lastPreparedAt = now,
                    isSelected = false,
                    isCustom = false,
                    notes = "Alternativa de baixa latência para resolução DNS protegida em redes Unitel."
                ),
                ServerProfileEntity(
                    name = "Servidor Próprio · Luanda WG",
                    host = "197.216.0.10",
                    port = 51820,
                    protocol = VpnProtocol.WIREGUARD_TUNNEL.name,
                    tunnelMode = TunnelMode.FULL_ENDPOINT_TUNNEL.name,
                    dnsPrimary = "1.1.1.1",
                    dnsSecondary = "8.8.8.8",
                    mtu = 1380,
                    preferredCarrier = "UNITEL",
                    isPreparedOffline = false,
                    isSelected = false,
                    isCustom = false,
                    notes = "Modelo configurável: edite este perfil com o IP público ou domínio real do seu servidor privado em Angola."
                ),
                ServerProfileEntity(
                    name = "Servidor Próprio · Africell Relay",
                    host = "102.130.0.20",
                    port = 1080,
                    protocol = VpnProtocol.SOCKS5_PROXY.name,
                    tunnelMode = TunnelMode.FULL_ENDPOINT_TUNNEL.name,
                    dnsPrimary = "1.1.1.1",
                    dnsSecondary = "9.9.9.9",
                    mtu = 1400,
                    preferredCarrier = "AFRICELL",
                    isPreparedOffline = false,
                    isSelected = false,
                    isCustom = false,
                    notes = "Modelo configurável para servidor SOCKS5/TCP dedicado. Actualize o endereço e porta antes de ligar."
                )
            )
        }
    }
}

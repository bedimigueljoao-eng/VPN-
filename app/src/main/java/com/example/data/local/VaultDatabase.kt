package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.TunnelMode
import com.example.model.VpnProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ServerProfileEntity::class, UsageSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

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
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.vaultDao()?.insertServers(defaultServers())
                        }
                    }
                })
                .build()
        }

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

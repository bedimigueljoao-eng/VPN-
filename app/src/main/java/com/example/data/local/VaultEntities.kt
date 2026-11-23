package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_profiles")
data class ServerProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val tunnelMode: String,
    val dnsPrimary: String,
    val dnsSecondary: String,
    val mtu: Int = 1420,
    val preferredCarrier: String = "ALL",
    val authUsername: String = "",
    val publicKeyOrToken: String = "",
    val isPreparedOffline: Boolean = false,
    val lastPreparedAt: Long? = null,
    val lastLatencyMs: Long? = null,
    val isSelected: Boolean = false,
    val isCustom: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "usage_sessions")
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val serverName: String,
    val serverHost: String,
    val carrierType: String,
    val transportType: String,
    val protocol: String,
    val vpnIp: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationSeconds: Long = 0L,
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val status: String = "ACTIVA"
)

data class CarrierUsageSummary(
    val carrierType: String,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val sessionCount: Int,
    val totalDurationSeconds: Long
)

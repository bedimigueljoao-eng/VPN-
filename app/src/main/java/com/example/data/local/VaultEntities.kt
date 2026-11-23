package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

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

@Entity(tableName = "connectivity_lab_reports")
data class ConnectivityLabReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val carrierTested: String,
    val detectedMccMnc: String,
    val transportType: String,
    val balanceState: String,
    val classification: String,
    val connectivitySummaryPt: String,
    val destinationsJson: String,
    val vpnActive: Boolean,
    val vpnServerName: String,
    val vpnIp: String,
    val publicIpBefore: String,
    val publicIpAfter: String,
    val dataConsumedBytes: Long,
    val technicalConclusionPt: String,
    val executedAtMillis: Long
)

@Entity(tableName = "documented_services")
data class DocumentedServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val carrierType: String,
    val serviceName: String,
    val domainOrHost: String,
    val port: Int = 443,
    val sourceReference: String,
    val verifiedDate: String,
    val conditionsPt: String,
    val validityNotePt: String,
    val isOfficiallyAuthorized: Boolean = false,
    val isUserAdded: Boolean = true
)

@JsonClass(generateAdapter = true)
data class LabDestinationRecord(
    val labelPt: String,
    val host: String,
    val port: Int,
    val categoryPt: String,
    val reachable: Boolean,
    val latencyMs: Long?
)

data class CarrierUsageSummary(
    val carrierType: String,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val sessionCount: Int,
    val totalDurationSeconds: Long
)

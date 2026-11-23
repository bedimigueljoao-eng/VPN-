package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM server_profiles ORDER BY isSelected DESC, isPreparedOffline DESC, id ASC")
    fun observeServers(): Flow<List<ServerProfileEntity>>

    @Query("SELECT * FROM server_profiles WHERE isSelected = 1 LIMIT 1")
    fun observeSelectedServer(): Flow<ServerProfileEntity?>

    @Query("SELECT * FROM server_profiles WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedServerOnce(): ServerProfileEntity?

    @Query("SELECT * FROM server_profiles WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Long): ServerProfileEntity?

    @Query("SELECT * FROM server_profiles ORDER BY id ASC")
    suspend fun getAllServersOnce(): List<ServerProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerProfileEntity>)

    @Update
    suspend fun updateServer(server: ServerProfileEntity)

    @Query("DELETE FROM server_profiles WHERE id = :id")
    suspend fun deleteServerById(id: Long)

    @Query("UPDATE server_profiles SET isSelected = 0")
    suspend fun clearSelectedServer()

    @Query("UPDATE server_profiles SET isSelected = 1 WHERE id = :id")
    suspend fun markServerSelected(id: Long)

    @Transaction
    suspend fun selectServer(id: Long) {
        clearSelectedServer()
        markServerSelected(id)
    }

    @Query(
        "UPDATE server_profiles SET isPreparedOffline = :prepared, lastPreparedAt = :timestamp, lastLatencyMs = :latencyMs WHERE id = :id"
    )
    suspend fun updateServerVaultStatus(
        id: Long,
        prepared: Boolean,
        timestamp: Long,
        latencyMs: Long?
    )

    @Query("UPDATE server_profiles SET lastLatencyMs = :latencyMs WHERE id = :id")
    suspend fun updateServerLatency(id: Long, latencyMs: Long?)

    @Query("SELECT * FROM usage_sessions ORDER BY startedAt DESC LIMIT 100")
    fun observeUsageSessions(): Flow<List<UsageSessionEntity>>

    @Query(
        """
        SELECT carrierType,
               COALESCE(SUM(rxBytes), 0) AS totalRxBytes,
               COALESCE(SUM(txBytes), 0) AS totalTxBytes,
               COUNT(*) AS sessionCount,
               COALESCE(SUM(durationSeconds), 0) AS totalDurationSeconds
        FROM usage_sessions
        GROUP BY carrierType
        """
    )
    fun observeCarrierUsageSummaries(): Flow<List<CarrierUsageSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UsageSessionEntity): Long

    @Query(
        """
        UPDATE usage_sessions
        SET endedAt = :endedAt,
            durationSeconds = :durationSeconds,
            rxBytes = :rxBytes,
            txBytes = :txBytes,
            status = :status
        WHERE id = :sessionId
        """
    )
    suspend fun finishSession(
        sessionId: Long,
        endedAt: Long,
        durationSeconds: Long,
        rxBytes: Long,
        txBytes: Long,
        status: String
    )

    @Query("DELETE FROM usage_sessions")
    suspend fun clearUsageHistory()

    // Laboratório de Conectividade
    @Query("SELECT * FROM connectivity_lab_reports ORDER BY executedAtMillis DESC LIMIT 100")
    fun observeLabReports(): Flow<List<ConnectivityLabReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabReport(report: ConnectivityLabReportEntity): Long

    @Query("DELETE FROM connectivity_lab_reports")
    suspend fun clearLabReports()

    @Query("SELECT * FROM documented_services ORDER BY carrierType ASC, id ASC")
    fun observeDocumentedServices(): Flow<List<DocumentedServiceEntity>>

    @Query("SELECT * FROM documented_services WHERE carrierType = :carrierType ORDER BY id ASC")
    suspend fun getDocumentedServicesByCarrier(carrierType: String): List<DocumentedServiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentedService(service: DocumentedServiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentedServices(services: List<DocumentedServiceEntity>)

    @Query("DELETE FROM documented_services WHERE id = :id")
    suspend fun deleteDocumentedServiceById(id: Long)
}

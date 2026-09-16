package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.OutboxEventEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity

// Persists queue transitions and sync diagnostics.
@Dao
interface SyncDao {
    @Query("SELECT * FROM outbox_event WHERE status IN ('PENDING','FAILED_RETRYABLE','RETRY') AND nextAttemptAtEpochMillis <= :now ORDER BY sequence LIMIT :limit")
    fun eligible(now: Long, limit: Int): List<OutboxEventEntity>
    @Query("UPDATE outbox_event SET status = :status, resultStatus = :result, incidentId = :incident, lastError = :message WHERE id = :id")
    fun terminal(id: String, status: String, result: String?, incident: String?, message: String?)
    @Query("UPDATE outbox_event SET status = 'IN_FLIGHT' WHERE id IN (:ids)")
    fun markInFlight(ids: List<String>)
    @Query("UPDATE outbox_event SET status = 'SYNCED', resultStatus = :result, incidentId = :incident, lastError = :message WHERE id = :id")
    fun markSynced(id: String, result: String?, incident: String?, message: String?)
    @Query("UPDATE outbox_event SET status = 'FAILED_RETRYABLE', attemptCount = attemptCount + 1, nextAttemptAtEpochMillis = :next, lastError = :message WHERE id = :id")
    fun markFailedRetryable(id: String, next: Long, message: String)
    @Query("UPDATE outbox_event SET status = 'FAILED_RETRYABLE', nextAttemptAtEpochMillis = :next WHERE status = 'IN_FLIGHT'")
    fun recoverInFlight(next: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun saveState(state: SyncStateEntity)
    @Query("SELECT * FROM sync_state WHERE id = 1") fun state(): SyncStateEntity?
}

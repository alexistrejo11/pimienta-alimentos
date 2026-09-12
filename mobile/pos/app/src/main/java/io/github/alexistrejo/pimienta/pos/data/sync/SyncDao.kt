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
    @Query("SELECT * FROM outbox_event WHERE status IN ('PENDING','RETRY') AND nextAttemptAtEpochMillis <= :now ORDER BY sequence LIMIT :limit")
    fun eligible(now: Long, limit: Int): List<OutboxEventEntity>
    @Query("UPDATE outbox_event SET status = :status, resultStatus = :result, incidentId = :incident, lastError = :message WHERE id = :id")
    fun terminal(id: String, status: String, result: String?, incident: String?, message: String?)
    @Query("UPDATE outbox_event SET status = 'RETRY', attemptCount = attemptCount + 1, nextAttemptAtEpochMillis = :next, lastError = :message WHERE id = :id")
    fun retry(id: String, next: Long, message: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun saveState(state: SyncStateEntity)
    @Query("SELECT * FROM sync_state WHERE id = 1") fun state(): SyncStateEntity?
}

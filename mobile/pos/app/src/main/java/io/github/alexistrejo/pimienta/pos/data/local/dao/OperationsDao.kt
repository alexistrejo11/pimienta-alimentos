package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.*

// Groups low-level persistence calls used by the transactional POS repository.
@Dao interface OperationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertDevice(device: DeviceEntity)
    @Query("SELECT * FROM device LIMIT 1") fun device(): DeviceEntity?
    @Query("SELECT * FROM shift WHERE status = 'OPEN' LIMIT 1") fun activeShift(): ShiftEntity?
    @Insert fun insertShift(shift: ShiftEntity)
    @Insert fun insertSale(sale: SaleEntity)
    @Insert fun insertLines(lines: List<SaleLineEntity>)
    @Insert fun insertPayment(payment: PaymentEntity)
    @Insert fun insertMovements(movements: List<InventoryMovementEntity>)
    @Insert fun insertOutbox(event: OutboxEventEntity)
    @Insert fun insertPrintJob(job: PrintJobEntity)
    @Query("UPDATE device SET nextEventSequence = :nextSequence WHERE id = :id") fun updateSequence(id: String, nextSequence: Long)
    @Query("UPDATE shift SET nextFolioNumber = :nextNumber WHERE id = :id") fun updateFolioNumber(id: String, nextNumber: Long)
    @Query("SELECT COUNT(*) FROM outbox_event WHERE status = 'PENDING'") fun pendingEventCount(): Int
}

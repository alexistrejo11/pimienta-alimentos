package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// Groups low-level persistence calls used by the transactional POS repository.
@Dao interface OperationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertDevice(device: DeviceEntity)
    @Query("SELECT * FROM device LIMIT 1") fun device(): DeviceEntity?
    @Query("SELECT * FROM shift WHERE status = 'OPEN' LIMIT 1") fun activeShift(): ShiftEntity?
    @Insert fun insertShift(shift: ShiftEntity)
    @Insert fun insertSale(sale: SaleEntity)
    @Insert fun insertLines(lines: List<SaleLineEntity>)
    @Insert fun insertPayment(payment: PaymentEntity)
    @Insert fun insertDiscount(discount: SaleDiscountEntity)
    @Insert fun insertWithdrawal(withdrawal: CashWithdrawalEntity)
    @Insert fun insertCashCountAttempt(attempt: CashCountAttemptEntity)
    @Insert fun insertShiftClose(close: ShiftCloseEntity)
    @Insert fun insertCancellation(cancellation: SaleCancellationEntity)
    @Insert fun insertMovements(movements: List<InventoryMovementEntity>)
    @Insert fun insertOutbox(event: OutboxEventEntity)
    @Insert fun insertPrintJob(job: PrintJobEntity)
    @Query("UPDATE device SET nextEventSequence = :nextSequence WHERE id = :id") fun updateSequence(id: String, nextSequence: Long)
    @Query("UPDATE shift SET nextFolioNumber = :nextNumber WHERE id = :id") fun updateFolioNumber(id: String, nextNumber: Long)
    @Query("UPDATE shift SET status = :status WHERE id = :id") fun updateShiftStatus(id: String, status: String)
    @Query("UPDATE cash_count_attempt SET status = :status, note = :note WHERE id = :id") fun updateCashCountStatus(id: String, status: String, note: String?)
    @Query("UPDATE sale SET status = 'CANCELLED' WHERE id = :saleId") fun markSaleCancelled(saleId: String)
     @Query("SELECT COUNT(*) FROM outbox_event WHERE status IN ('PENDING', 'IN_FLIGHT', 'FAILED_RETRYABLE', 'RETRY')") fun pendingEventCount(): Int
    @Query("SELECT COUNT(*) FROM outbox_event WHERE status IN ('PENDING', 'IN_FLIGHT', 'FAILED_RETRYABLE', 'RETRY')") fun observePendingEventCount(): Flow<Int>
    @Query("SELECT COALESCE(SUM(grossCentavos), 0) FROM sale WHERE shiftId = :shiftId AND status != 'CANCELLED'") fun grossForShift(shiftId: String): Long
    @Query("SELECT COALESCE(SUM(discountCentavos), 0) FROM sale WHERE shiftId = :shiftId AND status != 'CANCELLED'") fun discountsForShift(shiftId: String): Long
    @Query("SELECT COALESCE(SUM(totalCentavos), 0) FROM sale WHERE shiftId = :shiftId AND status != 'CANCELLED'") fun netForShift(shiftId: String): Long
    @Query("SELECT COUNT(*) FROM sale WHERE shiftId = :shiftId AND status != 'CANCELLED'") fun ticketCountForShift(shiftId: String): Int
    @Query("SELECT COUNT(*) FROM sale WHERE shiftId = :shiftId AND status = 'CANCELLED'") fun cancelledCountForShift(shiftId: String): Int
    @Query("SELECT COALESCE(SUM(totalCentavos), 0) FROM sale WHERE shiftId = :shiftId AND paymentMethod = 'CORTESIA' AND status != 'CANCELLED'") fun courtesyForShift(shiftId: String): Long
    @Query("SELECT COALESCE(SUM(amountCentavos), 0) FROM cash_withdrawal WHERE shiftId = :shiftId") fun withdrawalsForShift(shiftId: String): Long
    @Query("SELECT COALESCE(SUM(totalCentavos), 0) FROM sale WHERE shiftId = :shiftId AND paymentMethod = 'CASH' AND status != 'CANCELLED'") fun cashSalesForShift(shiftId: String): Long
    @Query("SELECT * FROM cash_withdrawal WHERE shiftId = :shiftId ORDER BY createdAtEpochMillis DESC") fun withdrawals(shiftId: String): List<CashWithdrawalEntity>
    @Query("SELECT * FROM sale WHERE shiftId = :shiftId ORDER BY confirmedAtEpochMillis DESC") fun salesForShift(shiftId: String): List<SaleEntity>
    @Query("SELECT * FROM sale WHERE id = :saleId LIMIT 1") fun sale(saleId: String): SaleEntity?
    @Query("SELECT * FROM cash_withdrawal WHERE id = :id LIMIT 1") fun withdrawal(id: String): CashWithdrawalEntity?
    @Query("SELECT * FROM shift_close WHERE id = :id LIMIT 1") fun shiftClose(id: String): ShiftCloseEntity?
    @Query("SELECT * FROM sale_line WHERE saleId = :saleId ORDER BY id") fun linesForSale(saleId: String): List<SaleLineEntity>
    @Query("SELECT * FROM inventory_movement WHERE createdAtEpochMillis BETWEEN :from AND :to ORDER BY createdAtEpochMillis DESC") fun movementsBetween(from: Long, to: Long): List<InventoryMovementEntity>
    @Query("SELECT * FROM sale WHERE confirmedAtEpochMillis BETWEEN :from AND :to ORDER BY confirmedAtEpochMillis DESC") fun salesBetween(from: Long, to: Long): List<SaleEntity>
    @Query("SELECT * FROM sale_line WHERE saleId IN (SELECT id FROM sale WHERE confirmedAtEpochMillis BETWEEN :from AND :to AND status != 'CANCELLED') ORDER BY subtotalCentavos DESC") fun linesBetween(from: Long, to: Long): List<SaleLineEntity>
    @Query("SELECT * FROM cash_withdrawal WHERE createdAtEpochMillis BETWEEN :from AND :to ORDER BY createdAtEpochMillis DESC") fun withdrawalsBetween(from: Long, to: Long): List<CashWithdrawalEntity>
    @Query("SELECT * FROM cash_count_attempt WHERE shiftId = :shiftId ORDER BY createdAtEpochMillis DESC") fun cashCounts(shiftId: String): List<CashCountAttemptEntity>
    @Query("SELECT * FROM print_job WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAtEpochMillis DESC") fun pendingPrintJobs(): List<PrintJobEntity>
    @Query("SELECT * FROM print_job WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAtEpochMillis ASC LIMIT 1") fun nextPrintJob(): PrintJobEntity?
    @Query("UPDATE print_job SET status = 'PRINTING', attemptCount = attemptCount + 1, lastAttemptAtEpochMillis = :attemptedAt, lastError = NULL WHERE id = :id AND status IN ('PENDING', 'FAILED')") fun markPrintJobPrinting(id: String, attemptedAt: Long): Int
    @Query("UPDATE print_job SET status = :status, lastError = :error WHERE id = :id") fun finishPrintJob(id: String, status: String, error: String?)
    @Query("SELECT * FROM inventory_movement WHERE createdAtEpochMillis BETWEEN :from AND :to ORDER BY createdAtEpochMillis DESC") fun inventoryMovementsBetween(from: Long, to: Long): List<InventoryMovementEntity>
    @Query("SELECT * FROM inventory_movement ORDER BY createdAtEpochMillis DESC LIMIT 20") fun recentInventoryMovements(): List<InventoryMovementEntity>
    @Query("SELECT COUNT(*) FROM sale_cancellation WHERE shiftId = :shiftId") fun cancellationCountForShift(shiftId: String): Int
    @Insert fun insertTelemetry(event: TelemetryEventEntity)
    @Query("SELECT * FROM telemetry_event ORDER BY createdAtEpochMillis LIMIT :limit") fun pendingTelemetry(limit: Int): List<TelemetryEventEntity>
    @Query("DELETE FROM telemetry_event WHERE id IN (:ids)") fun deleteTelemetry(ids: List<String>)
    @Query("DELETE FROM telemetry_event WHERE id IN (SELECT id FROM telemetry_event ORDER BY createdAtEpochMillis LIMIT -1 OFFSET :maxEntries)") fun trimTelemetry(maxEntries: Int)
    @Query("SELECT COUNT(*) FROM telemetry_event") fun telemetryCount(): Int
    @Query("SELECT MIN(createdAtEpochMillis) FROM outbox_event WHERE status IN ('PENDING','RETRY')") fun oldestPendingEventAt(): Long?
}

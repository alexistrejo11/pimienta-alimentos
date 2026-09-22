package io.github.alexistrejo.pimienta.pos.data.printing

import io.github.alexistrejo.pimienta.pos.data.local.dao.OperationsDao
import io.github.alexistrejo.pimienta.pos.data.local.entity.PrintJobEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests print job expiration rules for stale automatic tickets vs manual reprints.
class PrintJobExpirationTest {

    private val now = 1_700_000_000_000L
    private val tenMinutesAgo = now - (10 * 60 * 1000L)
    private val fortyMinutesAgo = now - (40 * 60 * 1000L)

    private val openShift = ShiftEntity("shift-open", "dev-1", "site-1", "user-1", 1000, tenMinutesAgo, "OPEN", 1)
    private val closedShift = ShiftEntity("shift-closed", "dev-1", "site-1", "user-1", 1000, fortyMinutesAgo, "CLOSED", 1)

    private val saleInOpenShift = SaleEntity("sale-open", "F1", "shift-open", "user-1", 1000, 0, 1000, "CASH", 1000, 0, tenMinutesAgo)
    private val saleInClosedShift = SaleEntity("sale-closed", "F2", "shift-closed", "user-1", 1000, 0, 1000, "CASH", 1000, 0, fortyMinutesAgo)

    private val fakeDao = object : FakeOperationsDao() {
        override fun sale(saleId: String): SaleEntity? = when (saleId) {
            "sale-open" -> saleInOpenShift
            "sale-closed" -> saleInClosedShift
            else -> null
        }

        override fun findShift(id: String): ShiftEntity? = when (id) {
            "shift-open" -> openShift
            "shift-closed" -> closedShift
            else -> null
        }
    }

    @Test
    fun freshJobWithin30MinutesInOpenShiftDoesNotExpire() {
        val freshJob = PrintJobEntity("job-1", "sale-open", "PENDING", duplicate = false, createdAtEpochMillis = tenMinutesAgo)

        assertFalse(PrintJobProcessor.shouldExpire(freshJob, fakeDao, now))
    }

    @Test
    fun jobOlderThan30MinutesExpires() {
        val oldJob = PrintJobEntity("job-old", "sale-open", "PENDING", duplicate = false, createdAtEpochMillis = fortyMinutesAgo)

        assertTrue(PrintJobProcessor.shouldExpire(oldJob, fakeDao, now))
    }

    @Test
    fun jobFromClosedShiftExpiresEvenIfWithin30Minutes() {
        val recentJobInClosedShift = PrintJobEntity("job-closed", "sale-closed", "PENDING", duplicate = false, createdAtEpochMillis = tenMinutesAgo)

        assertTrue(PrintJobProcessor.shouldExpire(recentJobInClosedShift, fakeDao, now))
    }

    @Test
    fun manualReprintDuplicateJobNeverExpiresEvenIfOldOrClosedShift() {
        val manualReprint = PrintJobEntity("job-reprint", "sale-closed", "PENDING", duplicate = true, createdAtEpochMillis = fortyMinutesAgo)

        assertFalse(PrintJobProcessor.shouldExpire(manualReprint, fakeDao, now))
    }
}

private open class FakeOperationsDao : OperationsDao {
    override fun insertDevice(device: io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity) {}
    override fun device(): io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity? = null
    override fun activeShift(): ShiftEntity? = null
    override fun insertShift(shift: ShiftEntity) {}
    override fun insertSale(sale: SaleEntity) {}
    override fun insertLines(lines: List<io.github.alexistrejo.pimienta.pos.data.local.entity.SaleLineEntity>) {}
    override fun insertPayment(payment: io.github.alexistrejo.pimienta.pos.data.local.entity.PaymentEntity) {}
    override fun insertDiscount(discount: io.github.alexistrejo.pimienta.pos.data.local.entity.SaleDiscountEntity) {}
    override fun insertWithdrawal(withdrawal: io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity) {}
    override fun insertCashCountAttempt(attempt: io.github.alexistrejo.pimienta.pos.data.local.entity.CashCountAttemptEntity) {}
    override fun insertShiftClose(close: io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftCloseEntity) {}
    override fun insertCancellation(cancellation: io.github.alexistrejo.pimienta.pos.data.local.entity.SaleCancellationEntity) {}
    override fun insertMovements(movements: List<io.github.alexistrejo.pimienta.pos.data.local.entity.InventoryMovementEntity>) {}
    override fun insertOutbox(event: io.github.alexistrejo.pimienta.pos.data.local.entity.OutboxEventEntity) {}
    override fun insertPrintJob(job: PrintJobEntity) {}
    override fun updateSequence(id: String, nextSequence: Long) {}
    override fun updateFolioNumber(id: String, nextNumber: Long) {}
    override fun updateShiftStatus(id: String, status: String) {}
    override fun updateCashCountStatus(id: String, status: String, note: String?) {}
    override fun markSaleCancelled(saleId: String) {}
    override fun pendingEventCount(): Int = 0
    override fun observePendingEventCount(): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.emptyFlow()
    override fun grossForShift(shiftId: String): Long = 0
    override fun discountsForShift(shiftId: String): Long = 0
    override fun netForShift(shiftId: String): Long = 0
    override fun ticketCountForShift(shiftId: String): Int = 0
    override fun cancelledCountForShift(shiftId: String): Int = 0
    override fun courtesyForShift(shiftId: String): Long = 0
    override fun withdrawalsForShift(shiftId: String): Long = 0
    override fun withdrawalCountForShift(shiftId: String): Int = 0
    override fun cashSalesForShift(shiftId: String): Long = 0
    override fun cardSalesForShift(shiftId: String): Long = 0
    override fun cancelledCashForShift(shiftId: String): Long = 0
    override fun lineAmountForShift(shiftId: String, lineType: String): Long = 0
    override fun lineQuantityForShift(shiftId: String, lineType: String): Int = 0
    override fun withdrawals(shiftId: String): List<io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity> = emptyList()
    override fun salesForShift(shiftId: String): List<SaleEntity> = emptyList()
    override fun sale(saleId: String): SaleEntity? = null
    override fun withdrawal(id: String): io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity? = null
    override fun shiftClose(id: String): io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftCloseEntity? = null
    override fun findShift(id: String): ShiftEntity? = null
    override fun findUser(id: String): io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity? = null
    override fun linesForSale(saleId: String): List<io.github.alexistrejo.pimienta.pos.data.local.entity.SaleLineEntity> = emptyList()
    override fun linesForShift(shiftId: String): List<io.github.alexistrejo.pimienta.pos.data.local.entity.SaleLineEntity> = emptyList()
    override fun movementsBetween(from: Long, to: Long): List<io.github.alexistrejo.pimienta.pos.data.local.entity.InventoryMovementEntity> = emptyList()
    override fun salesBetween(from: Long, to: Long): List<SaleEntity> = emptyList()
    override fun linesBetween(from: Long, to: Long): List<io.github.alexistrejo.pimienta.pos.data.local.entity.SaleLineEntity> = emptyList()
    override fun withdrawalsBetween(from: Long, to: Long): List<io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity> = emptyList()
    override fun cashCounts(shiftId: String): List<io.github.alexistrejo.pimienta.pos.data.local.entity.CashCountAttemptEntity> = emptyList()
    override fun pendingPrintJobs(): List<PrintJobEntity> = emptyList()
    override fun nextPrintJob(): PrintJobEntity? = null
    override fun markPrintJobPrinting(id: String, attemptedAt: Long): Int = 0
    override fun finishPrintJob(id: String, status: String, error: String?) {}
    override fun inventoryMovementsBetween(from: Long, to: Long): List<io.github.alexistrejo.pimienta.pos.data.local.entity.InventoryMovementEntity> = emptyList()
    override fun recentInventoryMovements(): List<io.github.alexistrejo.pimienta.pos.data.local.entity.InventoryMovementEntity> = emptyList()
    override fun cancellationCountForShift(shiftId: String): Int = 0
    override fun insertTelemetry(event: io.github.alexistrejo.pimienta.pos.data.local.entity.TelemetryEventEntity) {}
    override fun pendingTelemetry(limit: Int): List<io.github.alexistrejo.pimienta.pos.data.local.entity.TelemetryEventEntity> = emptyList()
    override fun deleteTelemetry(ids: List<String>) {}
    override fun trimTelemetry(maxEntries: Int) {}
    override fun telemetryCount(): Int = 0
    override fun oldestPendingEventAt(): Long? = null
}

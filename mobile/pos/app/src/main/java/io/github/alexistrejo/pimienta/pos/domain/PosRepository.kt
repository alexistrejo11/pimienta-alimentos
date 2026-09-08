package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// Represents an editable sale line before it becomes an immutable database snapshot.
data class CartLine(val productId: String, val name: String, val category: String, val unitPriceCentavos: Long, val stockPolicy: String, val quantity: Int)

// Represents the payment choice exposed by the first local checkout.
enum class PaymentMethod { CASH, EXTERNAL_CARD_MP, CORTESIA }

// Captures the manager approval that makes one draft discount eligible for confirmation.
data class SaleDiscountDraft(val amountCentavos: Long, val reason: String, val authorizedBy: LocalUserEntity)

// Provides the immutable totals shown by the local Corte Z summary.
data class ShiftTotals(val grossCentavos: Long, val discountsCentavos: Long, val netCentavos: Long, val courtesyCentavos: Long, val ticketCount: Int = 0, val cancelledCount: Int = 0)

// Contains the local operational metrics shown on the Manager dashboard.
data class DashboardSummary(
    val grossCentavos: Long,
    val discountsCentavos: Long,
    val netCentavos: Long,
    val ticketCount: Int,
    val averageTicketCentavos: Long,
    val cashCollectedCentavos: Long,
    val withdrawalsCentavos: Long,
    val withdrawalCount: Int,
    val cancelledCount: Int,
    val wasteCount: Int,
    val pendingEvents: Int,
    val topProducts: List<TopProductSummary>,
)

// Represents a compact top-product row without exposing database entities to Compose.
data class TopProductSummary(val name: String, val quantity: Int, val amountCentavos: Long)

// Converts and formats money without using floating point values.
object Money {
    fun fromCatalog(value: String): Long = BigDecimal(value).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    fun fromInput(value: String): Long? = value.toBigDecimalOrNull()?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()
    fun format(centavos: Long): String = "$" + BigDecimal.valueOf(centavos, 2).setScale(2).toPlainString()
}

// Holds the local rules and atomic persistence needed by the Phase 1 POS flow.
class PosRepository(private val database: PosDatabase) {
    fun users() = database.userDao().activeUsers()
    fun products() = database.productDao().getAll()
    fun activeShift() = database.operationsDao().activeShift()
    fun pendingEvents() = database.operationsDao().pendingEventCount()
    fun shiftTotals(shiftId: String): ShiftTotals = database.operationsDao().let { dao -> ShiftTotals(dao.grossForShift(shiftId), dao.discountsForShift(shiftId), dao.netForShift(shiftId), dao.courtesyForShift(shiftId), dao.ticketCountForShift(shiftId), dao.cancelledCountForShift(shiftId)) }
    fun withdrawals(shiftId: String) = database.operationsDao().withdrawals(shiftId)
    fun withdrawalTotal(shiftId: String) = database.operationsDao().withdrawalsForShift(shiftId)
    fun expectedCash(shift: ShiftEntity): Long = database.operationsDao().let { shift.openingCashCentavos + it.cashSalesForShift(shift.id) - it.withdrawalsForShift(shift.id) }
    fun salesForShift(shiftId: String) = database.operationsDao().salesForShift(shiftId)
    fun linesForSale(saleId: String) = database.operationsDao().linesForSale(saleId)
    fun inventoryMovements(from: Long, to: Long) = database.operationsDao().inventoryMovementsBetween(from, to)
    fun recentInventoryMovements() = database.operationsDao().recentInventoryMovements()
    fun pendingPrintJobs() = database.operationsDao().pendingPrintJobs()
    fun dailySummary(now: Long = System.currentTimeMillis()): DashboardSummary {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val dao = database.operationsDao()
        val sales = dao.salesBetween(from, to)
        val validSales = sales.filter { it.status != "CANCELLED" }
        val gross = validSales.sumOf { it.grossCentavos }
        val discounts = validSales.sumOf { it.discountCentavos }
        val net = validSales.sumOf { it.totalCentavos }
        val withdrawals = dao.withdrawalsBetween(from, to)
        val movements = dao.inventoryMovementsBetween(from, to)
        val top = dao.linesBetween(from, to).groupBy { it.productName }.map { (name, lines) ->
            TopProductSummary(name, lines.sumOf { it.quantity }, lines.sumOf { it.subtotalCentavos })
        }.sortedWith(compareByDescending<TopProductSummary> { it.quantity }.thenByDescending { it.amountCentavos }).take(5)
        return DashboardSummary(gross, discounts, net, validSales.size, if (validSales.isEmpty()) 0 else net / validSales.size, validSales.filter { it.paymentMethod == "CASH" }.sumOf { it.totalCentavos }, withdrawals.sumOf { it.amountCentavos }, withdrawals.size, sales.count { it.status == "CANCELLED" }, movements.count { it.movementType == "WASTE" }, dao.pendingEventCount(), top)
    }
    fun authenticate(userId: String, pin: String): Boolean = database.userDao().find(userId)?.let { it.active && it.pinHash == hashPin(pin) } ?: false
    fun openShift(userId: String, openingCashCentavos: Long): ShiftEntity? = database.runInTransaction<ShiftEntity?> {
        val operations = database.operationsDao()
        operations.activeShift()?.let { return@runInTransaction it }
        val device = operations.device() ?: return@runInTransaction null
        val shift = ShiftEntity(UUID.randomUUID().toString(), device.id, "site-debug-001", userId, openingCashCentavos, System.currentTimeMillis(), "OPEN", 1)
        operations.insertShift(shift)
        shift
    }
    // Persists an authorized cash safeguard withdrawal with its print and sync work.
    fun recordWithdrawal(shift: ShiftEntity, amountCentavos: Long, authorizedById: String, pin: String): CashWithdrawalEntity? {
        if (amountCentavos <= 0) return null
        val authorizer = database.userDao().find(authorizedById) ?: return null
        if (!authorizer.active || (authorizer.role != "MANAGER" && authorizer.role != "SUPERADMIN") || !authenticate(authorizedById, pin)) return null
        return database.runInTransaction<CashWithdrawalEntity?> {
            val operations = database.operationsDao()
            val liveShift = operations.activeShift() ?: return@runInTransaction null
            if (liveShift.id != shift.id) return@runInTransaction null
            val device = operations.device() ?: return@runInTransaction null
            val id = UUID.randomUUID().toString()
            val folio = "SG-${device.visibleCode}-${liveShift.id.take(4).uppercase()}-${device.nextEventSequence.toString().padStart(4, '0')}"
            val withdrawal = CashWithdrawalEntity(id, folio, liveShift.id, liveShift.cashierId, amountCentavos, "RESGUARDO_EFECTIVO", authorizer.id, authorizer.role, System.currentTimeMillis())
            operations.insertWithdrawal(withdrawal)
            operations.insertOutbox(OutboxEventEntity(UUID.randomUUID().toString(), device.nextEventSequence, "CASH_WITHDRAWAL_RECORDED", id, "PENDING", System.currentTimeMillis()))
            operations.insertPrintJob(PrintJobEntity(UUID.randomUUID().toString(), id, "PENDING", false, System.currentTimeMillis(), "CASH_WITHDRAWAL"))
            operations.updateSequence(device.id, device.nextEventSequence + 1)
            withdrawal
        }
    }

    // Persists a local restock or waste movement and updates controlled stock immediately.
    fun recordInventoryMovement(shift: ShiftEntity, product: ProductEntity, quantity: Int, type: String, reason: String): Boolean {
        if (quantity <= 0 || reason.isBlank()) return false
        return database.runInTransaction<Boolean> {
            val operations = database.operationsDao()
            val device = operations.device() ?: return@runInTransaction false
            val movementId = UUID.randomUUID().toString()
            val delta = if (type == "WASTE") -quantity else quantity
            operations.insertMovements(listOf(InventoryMovementEntity(movementId, movementId, product.id, delta, System.currentTimeMillis(), type)))
            if (product.stockPolicy == "CONTROLLED") {
                val current = product.stock.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                database.productDao().updateStock(product.id, current.add(java.math.BigDecimal.valueOf(delta.toLong())).toPlainString())
            }
            operations.insertOutbox(OutboxEventEntity(UUID.randomUUID().toString(), device.nextEventSequence, if (type == "WASTE") "WASTE_RECORDED" else "RESTOCK_RECORDED", movementId, "PENDING", System.currentTimeMillis()))
            operations.updateSequence(device.id, device.nextEventSequence + 1)
            true
        }
    }

    // Stores a cashier's blind count and keeps it available for Manager review.
    fun submitCashCount(shift: ShiftEntity, totalCentavos: Long, denominations: String): CashCountAttemptEntity? {
        if (totalCentavos < 0) return null
        return database.runInTransaction<CashCountAttemptEntity?> {
            val operations = database.operationsDao()
            if (operations.activeShift()?.id != shift.id) return@runInTransaction null
            val attempt = CashCountAttemptEntity(UUID.randomUUID().toString(), shift.id, shift.cashierId, totalCentavos, denominations, "SUBMITTED", null, System.currentTimeMillis())
            operations.insertCashCountAttempt(attempt)
            val device = operations.device() ?: return@runInTransaction attempt
            operations.insertOutbox(OutboxEventEntity(UUID.randomUUID().toString(), device.nextEventSequence, "CASH_COUNT_SUBMITTED", attempt.id, "PENDING", System.currentTimeMillis()))
            operations.updateSequence(device.id, device.nextEventSequence + 1)
            attempt
        }
    }

    // Rejects a count with an audit note so the cashier can submit a new attempt.
    fun rejectCashCount(attemptId: String, note: String): Boolean {
        if (note.isBlank()) return false
        database.operationsDao().updateCashCountStatus(attemptId, "REJECTED", note.trim())
        return true
    }

    // Approves the latest count, seals the shift and creates its durable print/sync work.
    fun approveShiftClose(shift: ShiftEntity, attempt: CashCountAttemptEntity, manager: LocalUserEntity, pin: String): Boolean {
        if (!authenticate(manager.id, pin) || (manager.role != "MANAGER" && manager.role != "SUPERADMIN")) return false
        return database.runInTransaction<Boolean> {
            val operations = database.operationsDao()
            if (operations.activeShift()?.id != shift.id) return@runInTransaction false
            val close = ShiftCloseEntity(UUID.randomUUID().toString(), shift.id, expectedCash(shift), attempt.totalCentavos, attempt.totalCentavos - expectedCash(shift), manager.id, System.currentTimeMillis())
            operations.insertShiftClose(close)
            operations.updateCashCountStatus(attempt.id, "APPROVED", null)
            operations.updateShiftStatus(shift.id, "CLOSED")
            val device = operations.device() ?: return@runInTransaction false
            operations.insertOutbox(OutboxEventEntity(UUID.randomUUID().toString(), device.nextEventSequence, "SHIFT_CLOSED", close.id, "PENDING", System.currentTimeMillis()))
            operations.insertPrintJob(PrintJobEntity(UUID.randomUUID().toString(), close.id, "PENDING", false, System.currentTimeMillis(), "SHIFT_CLOSE"))
            operations.updateSequence(device.id, device.nextEventSequence + 1)
            true
        }
    }

    // Queues a reprint from durable ticket data without creating a new sale.
    fun requestReprint(documentId: String, documentType: String = "SALE") {
        database.operationsDao().insertPrintJob(
            PrintJobEntity(UUID.randomUUID().toString(), documentId, "PENDING", true, System.currentTimeMillis(), documentType)
        )
    }

    // Cancels only a cash sale from the active shift and records the full audit trail.
    fun cancelCashSale(sale: SaleEntity, manager: LocalUserEntity, pin: String, reason: String): Boolean {
        if (sale.status == "CANCELLED" || sale.paymentMethod != PaymentMethod.CASH.name || reason.isBlank()) return false
        if ((manager.role != "MANAGER" && manager.role != "SUPERADMIN") || !authenticate(manager.id, pin)) return false
        return database.runInTransaction<Boolean> {
            val operations = database.operationsDao()
            val liveShift = operations.activeShift() ?: return@runInTransaction false
            if (liveShift.id != sale.shiftId) return@runInTransaction false
            val lines = operations.linesForSale(sale.id)
            val device = operations.device() ?: return@runInTransaction false
            operations.markSaleCancelled(sale.id)
            operations.insertCancellation(SaleCancellationEntity(UUID.randomUUID().toString(), sale.id, sale.shiftId, reason.trim(), manager.id, manager.role, System.currentTimeMillis()))
            operations.insertMovements(lines.filter { it.stockPolicy == "CONTROLLED" }.map { line -> InventoryMovementEntity(UUID.randomUUID().toString(), sale.id, line.productId, line.quantity, System.currentTimeMillis(), "SALE_CANCELLATION") })
            lines.filter { it.stockPolicy == "CONTROLLED" }.forEach { line -> adjustStock(line.productId, line.quantity) }
            operations.insertOutbox(OutboxEventEntity(UUID.randomUUID().toString(), device.nextEventSequence, "SALE_CANCELLED", sale.id, "PENDING", System.currentTimeMillis()))
            operations.updateSequence(device.id, device.nextEventSequence + 1)
            true
        }
    }

    fun confirmSale(shift: ShiftEntity, lines: List<CartLine>, method: PaymentMethod, tenderedCentavos: Long, discount: SaleDiscountDraft? = null): SaleEntity? {
        if (lines.isEmpty()) return null
        val gross = lines.sumOf { it.unitPriceCentavos * it.quantity }
        if (discount != null && (discount.amountCentavos <= 0 || discount.amountCentavos > gross || discount.reason.isBlank() || (discount.authorizedBy.role != "MANAGER" && discount.authorizedBy.role != "SUPERADMIN"))) return null
        val total = gross - (discount?.amountCentavos ?: 0)
        if (total == 0L && (method != PaymentMethod.CORTESIA || discount?.amountCentavos != gross)) return null
        if (total > 0L && method == PaymentMethod.CORTESIA) return null
        if (method == PaymentMethod.CASH && tenderedCentavos < total) return null
        return database.runInTransaction<SaleEntity?> {
            val operations = database.operationsDao()
            val liveShift = operations.activeShift() ?: return@runInTransaction null
            if (liveShift.id != shift.id) return@runInTransaction null
            val device = operations.device() ?: return@runInTransaction null
            val saleId = UUID.randomUUID().toString()
            val change = if (method == PaymentMethod.CASH) tenderedCentavos - total else 0
            val folio = "${device.visibleCode}-${liveShift.id.take(4).uppercase()}-${liveShift.nextFolioNumber.toString().padStart(4, '0')}"
            val sale = SaleEntity(saleId, folio, liveShift.id, liveShift.cashierId, gross, discount?.amountCentavos ?: 0, total, method.name, if (method == PaymentMethod.CASH) tenderedCentavos else total, change, System.currentTimeMillis())
            operations.insertSale(sale)
            discount?.let { operations.insertDiscount(SaleDiscountEntity(UUID.randomUUID().toString(), saleId, it.amountCentavos, it.reason.trim(), it.authorizedBy.id, it.authorizedBy.role, System.currentTimeMillis())) }
            operations.insertLines(lines.map { line -> SaleLineEntity(UUID.randomUUID().toString(), saleId, line.productId, line.name, line.category, line.quantity, line.unitPriceCentavos, line.unitPriceCentavos * line.quantity, line.stockPolicy) })
            operations.insertPayment(PaymentEntity(UUID.randomUUID().toString(), saleId, method.name, total))
            operations.insertMovements(lines.filter { it.stockPolicy == "CONTROLLED" }.map { line -> InventoryMovementEntity(UUID.randomUUID().toString(), saleId, line.productId, -line.quantity, System.currentTimeMillis()) })
            lines.filter { it.stockPolicy == "CONTROLLED" }.forEach { line -> adjustStock(line.productId, -line.quantity) }
            operations.insertOutbox(OutboxEventEntity(UUID.randomUUID().toString(), device.nextEventSequence, "SALE_CONFIRMED", saleId, "PENDING", System.currentTimeMillis()))
            operations.insertPrintJob(PrintJobEntity(UUID.randomUUID().toString(), saleId, "PENDING", false, System.currentTimeMillis()))
            operations.updateSequence(device.id, device.nextEventSequence + 1)
            operations.updateFolioNumber(liveShift.id, liveShift.nextFolioNumber + 1)
            sale
        }
    }
    // Applies a local stock delta while preserving decimal quantities in the catalog snapshot.
    private fun adjustStock(productId: String, delta: Int) {
        val product = database.productDao().findById(productId) ?: return
        val current = product.stock.toBigDecimalOrNull() ?: BigDecimal.ZERO
        database.productDao().updateStock(productId, current.add(BigDecimal.valueOf(delta.toLong())).toPlainString())
    }
    private fun hashPin(pin: String): String = MessageDigest.getInstance("SHA-256").digest("pimienta-debug|$pin".toByteArray()).joinToString("") { "%02x".format(it) }
}

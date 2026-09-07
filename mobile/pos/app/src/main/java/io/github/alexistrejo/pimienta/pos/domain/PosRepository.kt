package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.UUID

// Represents an editable sale line before it becomes an immutable database snapshot.
data class CartLine(val productId: String, val name: String, val category: String, val unitPriceCentavos: Long, val stockPolicy: String, val quantity: Int)

// Represents the payment choice exposed by the first local checkout.
enum class PaymentMethod { CASH, EXTERNAL_CARD_MP }

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
    fun authenticate(userId: String, pin: String): Boolean = database.userDao().find(userId)?.let { it.active && it.pinHash == hashPin(pin) } ?: false
    fun openShift(userId: String, openingCashCentavos: Long): ShiftEntity? = database.runInTransaction<ShiftEntity?> {
        val operations = database.operationsDao()
        operations.activeShift()?.let { return@runInTransaction it }
        val device = operations.device() ?: return@runInTransaction null
        val shift = ShiftEntity(UUID.randomUUID().toString(), device.id, "site-debug-001", userId, openingCashCentavos, System.currentTimeMillis(), "OPEN", 1)
        operations.insertShift(shift)
        shift
    }
    fun confirmSale(shift: ShiftEntity, lines: List<CartLine>, method: PaymentMethod, tenderedCentavos: Long): SaleEntity? {
        if (lines.isEmpty()) return null
        val total = lines.sumOf { it.unitPriceCentavos * it.quantity }
        if (method == PaymentMethod.CASH && tenderedCentavos < total) return null
        return database.runInTransaction<SaleEntity?> {
            val operations = database.operationsDao()
            val liveShift = operations.activeShift() ?: return@runInTransaction null
            if (liveShift.id != shift.id) return@runInTransaction null
            val device = operations.device() ?: return@runInTransaction null
            val saleId = UUID.randomUUID().toString()
            val change = if (method == PaymentMethod.CASH) tenderedCentavos - total else 0
            val folio = "${device.visibleCode}-${liveShift.id.take(4).uppercase()}-${liveShift.nextFolioNumber.toString().padStart(4, '0')}"
            val sale = SaleEntity(saleId, folio, liveShift.id, liveShift.cashierId, total, method.name, if (method == PaymentMethod.CASH) tenderedCentavos else total, change, System.currentTimeMillis())
            operations.insertSale(sale)
            operations.insertLines(lines.map { line -> SaleLineEntity(UUID.randomUUID().toString(), saleId, line.productId, line.name, line.category, line.quantity, line.unitPriceCentavos, line.unitPriceCentavos * line.quantity, line.stockPolicy) })
            operations.insertPayment(PaymentEntity(UUID.randomUUID().toString(), saleId, method.name, total))
            operations.insertMovements(lines.filter { it.stockPolicy == "CONTROLLED" }.map { line -> InventoryMovementEntity(UUID.randomUUID().toString(), saleId, line.productId, -line.quantity, System.currentTimeMillis()) })
            operations.insertOutbox(OutboxEventEntity(UUID.randomUUID().toString(), device.nextEventSequence, "SALE_CONFIRMED", saleId, "PENDING", System.currentTimeMillis()))
            operations.insertPrintJob(PrintJobEntity(UUID.randomUUID().toString(), saleId, "PENDING", false, System.currentTimeMillis()))
            operations.updateSequence(device.id, device.nextEventSequence + 1)
            operations.updateFolioNumber(liveShift.id, liveShift.nextFolioNumber + 1)
            sale
        }
    }
    private fun hashPin(pin: String): String = MessageDigest.getInstance("SHA-256").digest("pimienta-debug|$pin".toByteArray()).joinToString("") { "%02x".format(it) }
}

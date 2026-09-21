package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.dao.OperationsDao
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import io.github.alexistrejo.pimienta.pos.data.sync.OutboxPayloadBuilder
import io.github.alexistrejo.pimienta.pos.data.sync.PinVerifier
import io.github.alexistrejo.pimienta.pos.data.sync.trainingProductEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// Distinguishes catalog lines from unknown-barcode exceptions captured at the register.
@Serializable
enum class SaleLineType { CATALOG, PENDING_CATALOG, OPEN_AMOUNT }

// Represents an editable sale line before it becomes an immutable database snapshot.
@Serializable
data class CartLine(
    val productId: String?,
    val name: String,
    val category: String,
    val unitPriceCentavos: Long,
    val stockPolicy: String,
    val quantity: Int,
    val lineType: SaleLineType = SaleLineType.CATALOG,
    val sourceBarcode: String? = null,
    val authorizedByOperatorId: Long? = null,
    val authorizedAtEpochMillis: Long? = null,
    val cartLineId: String = "",
    val authorizedByUserId: String? = null,
) {
    // Stable cart identity for catalog ids or pending barcodes. Open amounts stay unique.
    val lineKey: String
        get() = when (lineType) {
            SaleLineType.CATALOG -> productId.orEmpty()
            SaleLineType.PENDING_CATALOG -> "pending:${sourceBarcode.orEmpty()}:${unitPriceCentavos}"
            SaleLineType.OPEN_AMOUNT -> "open:${cartLineId.ifBlank { "$category:$unitPriceCentavos:$authorizedAtEpochMillis" }}"
        }

    val subtotalCentavos: Long get() = SaleCalculator.lineSubtotalCentavos(unitPriceCentavos, quantity)
}

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

// Converts and formats money as integer centavos so POS totals never use floating point.
object Money {
    // Catalog prices are decimal peso strings persisted from Device API centavos.
    fun fromCatalog(value: String): Long =
        fromInput(value) ?: error("Precio de catálogo inválido: $value")

    // Accepts keypad or typed pesos with at most two decimals. Commas are treated as the decimal mark.
    fun fromInput(value: String): Long? {
        val normalized = value.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized == "." || normalized.contains('e', ignoreCase = true)) return null
        val fraction = normalized.substringAfter('.', missingDelimiterValue = "")
        if (normalized.count { it == '.' } > 1 || fraction.length > 2) return null
        val decimal = normalized.toBigDecimalOrNull() ?: return null
        if (decimal.signum() < 0) return null
        return runCatching {
            decimal.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        }.getOrNull()
    }

    // Ticket-safe amount without a currency symbol, including shortages such as -20.50.
    fun formatAmount(centavos: Long): String {
        val sign = if (centavos < 0) "-" else ""
        val absolute = if (centavos == Long.MIN_VALUE) Long.MAX_VALUE else if (centavos < 0) -centavos else centavos
        return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
    }

    fun format(centavos: Long): String {
        val amount = formatAmount(centavos)
        return if (amount.startsWith("-")) "-\$${amount.drop(1)}" else "\$$amount"
    }

    // Rounds the mean ticket to the nearest centavo instead of truncating toward zero.
    fun averageCentavos(totalCentavos: Long, count: Int): Long {
        if (count <= 0) return 0
        return BigDecimal.valueOf(totalCentavos)
            .divide(BigDecimal.valueOf(count.toLong()), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}

// Holds the local rules and atomic persistence needed by the Phase 1 POS flow.
class PosRepository(private val provider: PosDatabaseProvider, private val mode: RuntimeMode = provider.modes.mode()) {
    private val database get() = provider.database(mode)
    fun mode() = mode
    fun syncState() = database.syncDao().state()
    fun users() = database.userDao().activeUsers()
    fun products() = database.productDao().getAll()
    fun observeProducts(): Flow<List<ProductEntity>> = database.productDao().observeAll()
    fun observeUsers(): Flow<List<LocalUserEntity>> = database.userDao().observeActiveUsers()
    fun observePolicy(): Flow<PosPolicyEntity?> = database.syncProjectionDao().observePolicy()
    fun observeSyncState(): Flow<SyncStateEntity?> = database.syncDao().observeState()
    fun observePendingEvents(): Flow<Int> = database.operationsDao().observePendingEventCount()
    fun saleCategoryNames(): List<String> {
        val siteId = database.siteDao().current()?.id ?: return emptyList()
        return database.syncProjectionDao().activeCategories(siteId).map { it.name }.filter { it.isNotBlank() }
    }
    fun findProductByCode(code: String): ProductEntity? = database.productDao().findByCode(code.trim())
    fun activeShift() = database.operationsDao().activeShift()
    fun pendingEvents() = database.operationsDao().pendingEventCount()
    // Reads the locally cached open-amount policy without contacting the server.
    fun openAmountCategories(): List<String> = database.syncProjectionDao().policy()?.let {
        runCatching {
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(it.openAmountCategoriesJson)
        }.getOrDefault(emptyList())
    } ?: emptyList()
    fun allowOpenProducts(): Boolean = database.syncProjectionDao().policy()?.allowOpenProducts == true
    fun shiftTotals(shiftId: String): ShiftTotals = database.operationsDao().let { dao -> ShiftTotals(dao.grossForShift(shiftId), dao.discountsForShift(shiftId), dao.netForShift(shiftId), dao.courtesyForShift(shiftId), dao.ticketCountForShift(shiftId), dao.cancelledCountForShift(shiftId)) }
    fun withdrawals(shiftId: String) = database.operationsDao().withdrawals(shiftId)
    fun withdrawalTotal(shiftId: String) = database.operationsDao().withdrawalsForShift(shiftId)
    // Builds the Corte Z snapshot used by Manager UI, print, and the sealed close row.
    fun shiftCloseBreakdown(shift: ShiftEntity): ShiftCloseBreakdown {
        val dao = database.operationsDao()
        return ShiftCloseCalculator.breakdown(
            openingCashCentavos = shift.openingCashCentavos,
            cashSalesCentavos = dao.cashSalesForShift(shift.id),
            cardSalesCentavos = dao.cardSalesForShift(shift.id),
            courtesyGrossCentavos = dao.courtesyForShift(shift.id),
            discountsCentavos = dao.discountsForShift(shift.id),
            grossCentavos = dao.grossForShift(shift.id),
            netCentavos = dao.netForShift(shift.id),
            withdrawalsCentavos = dao.withdrawalsForShift(shift.id),
            withdrawalCount = dao.withdrawalCountForShift(shift.id),
            cancelledCount = dao.cancelledCountForShift(shift.id),
            cancelledCashCentavos = dao.cancelledCashForShift(shift.id),
            ticketCount = dao.ticketCountForShift(shift.id),
            catalogCentavos = dao.lineAmountForShift(shift.id, SaleLineType.CATALOG.name),
            openAmountCentavos = dao.lineAmountForShift(shift.id, SaleLineType.OPEN_AMOUNT.name),
            openAmountQuantity = dao.lineQuantityForShift(shift.id, SaleLineType.OPEN_AMOUNT.name),
            pendingCatalogCentavos = dao.lineAmountForShift(shift.id, SaleLineType.PENDING_CATALOG.name),
            pendingCatalogQuantity = dao.lineQuantityForShift(shift.id, SaleLineType.PENDING_CATALOG.name),
        )
    }
    fun expectedCash(shift: ShiftEntity): Long = shiftCloseBreakdown(shift).expectedCashCentavos
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
        return DashboardSummary(
            gross,
            discounts,
            net,
            validSales.size,
            Money.averageCentavos(net, validSales.size),
            validSales.filter { it.paymentMethod == "CASH" }.sumOf { it.totalCentavos },
            withdrawals.sumOf { it.amountCentavos },
            withdrawals.size,
            sales.count { it.status == "CANCELLED" },
            movements.count { it.movementType == "WASTE" },
            dao.pendingEventCount(),
            top,
        )
    }
    fun authenticate(userId: String, pin: String): Boolean =
        database.userDao().find(userId)?.let { it.active && PinVerifier.matches(pin, it.pinHash, mode == RuntimeMode.SANDBOX) } ?: false

    // Saves a product only in the training scratch DB so cashiers can practice the create flow.
    fun createTrainingProduct(
        name: String,
        salePriceCentavos: Long,
        saleCategory: String,
        barcode: String?,
        controlledStock: Boolean,
    ): Result<ProductEntity> {
        if (mode != RuntimeMode.SANDBOX) {
            return Result.failure(IllegalStateException("Solo capacitación guarda productos en local."))
        }
        val trimmedBarcode = barcode?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmedBarcode != null && database.productDao().findByCode(trimmedBarcode) != null) {
            return Result.failure(IllegalStateException("Ya existe un producto con ese código de barras."))
        }
        val product = trainingProductEntity(
            name = name.trim(),
            saleCategory = saleCategory.trim(),
            salePriceCentavos = salePriceCentavos,
            barcode = trimmedBarcode,
            controlledStock = controlledStock,
            id = UUID.randomUUID().toString(),
        )
        database.runInTransaction {
            database.productDao().insertAll(listOf(product))
            val siteId = database.siteDao().current()?.id
            if (siteId != null && product.saleCategory.isNotBlank()) {
                database.syncProjectionDao().insertCategories(listOf(CatalogCategoryEntity(siteId, product.saleCategory)))
            }
        }
        return Result.success(product)
    }

    // Opens the single allowed shift and records a durable SHIFT_OPENED sync event.
    fun openShift(userId: String, openingCashCentavos: Long): ShiftEntity? = database.runInTransaction<ShiftEntity?> {
        val operations = database.operationsDao()
        operations.activeShift()?.let { return@runInTransaction it }
        val device = operations.device() ?: return@runInTransaction null
        val siteId = device.siteId ?: database.siteDao().current()?.id ?: return@runInTransaction null
        val openedAt = System.currentTimeMillis()
        val shift = ShiftEntity(UUID.randomUUID().toString(), device.id, siteId, userId, openingCashCentavos, openedAt, "OPEN", 1)
        operations.insertShift(shift)
        enqueueOutbox(operations, device, siteId, shift.id, "SHIFT_OPENED", shift.id, OutboxPayloadBuilder.shiftOpened(shift), openedAt)
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
            enqueueOutbox(
                operations, device, liveShift.siteId, liveShift.id, "CASH_WITHDRAWAL_RECORDED", id,
                OutboxPayloadBuilder.cashWithdrawalRecorded(withdrawal), withdrawal.createdAtEpochMillis
            )
            operations.insertPrintJob(PrintJobEntity(UUID.randomUUID().toString(), id, "PENDING", false, System.currentTimeMillis(), "CASH_WITHDRAWAL"))
            withdrawal
        }
    }

    // Persists a local restock or waste movement and updates controlled stock immediately.
    fun recordInventoryMovement(shift: ShiftEntity, product: ProductEntity, quantity: Int, type: String, reason: String): Boolean {
        if (quantity <= 0 || reason.isBlank()) return false
        return database.runInTransaction<Boolean> {
            val operations = database.operationsDao()
            val liveShift = operations.activeShift() ?: return@runInTransaction false
            if (liveShift.id != shift.id) return@runInTransaction false
            val device = operations.device() ?: return@runInTransaction false
            val movementId = UUID.randomUUID().toString()
            val eventId = UUID.randomUUID().toString()
            val createdAt = System.currentTimeMillis()
            val delta = if (type == "WASTE") -quantity else quantity
            val movement = InventoryMovementEntity(movementId, movementId, product.id, delta, createdAt, type, eventId)
            operations.insertMovements(listOf(movement))
            val eventType = if (type == "WASTE") "WASTE_RECORDED" else "RESTOCK_RECORDED"
            val payload = if (type == "WASTE") OutboxPayloadBuilder.wasteRecorded(movement, product, reason.trim())
            else OutboxPayloadBuilder.restockRecorded(movement, product, reason.trim())
            enqueueOutbox(operations, device, liveShift.siteId, liveShift.id, eventType, movementId, payload, createdAt, eventId)
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
            enqueueOutbox(
                operations, device, shift.siteId, shift.id, "CASH_COUNT_SUBMITTED", attempt.id,
                OutboxPayloadBuilder.cashCountSubmitted(attempt), attempt.createdAtEpochMillis
            )
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
    fun approveShiftClose(shift: ShiftEntity, attempt: CashCountAttemptEntity, manager: LocalUserEntity, pin: String, printTicket: Boolean = true): Result<Boolean> = runCatching {
        if (!manager.active) {
            throw IllegalArgumentException("El usuario ${manager.displayName} no se encuentra activo.")
        }
        if (manager.role != "MANAGER" && manager.role != "SUPERADMIN") {
            throw IllegalArgumentException("El perfil de ${manager.displayName} no tiene permisos de Manager o Superadmin.")
        }
        if (!authenticate(manager.id, pin)) {
            throw IllegalArgumentException("PIN de ${manager.displayName} incorrecto.")
        }
        val closed = database.runInTransaction<Boolean> {
            val operations = database.operationsDao()
            if (operations.activeShift()?.id != shift.id) return@runInTransaction false
            val breakdown = shiftCloseBreakdown(shift)
            if (!breakdown.commercialMixIsConsistent() || !breakdown.lineMixIsConsistent()) return@runInTransaction false
            val expected = breakdown.expectedCashCentavos
            val close = ShiftCloseEntity(UUID.randomUUID().toString(), shift.id, expected, attempt.totalCentavos, ShiftCloseCalculator.differenceCentavos(attempt.totalCentavos, expected), manager.id, System.currentTimeMillis())
            operations.insertShiftClose(close)
            operations.updateCashCountStatus(attempt.id, "APPROVED", null)
            operations.updateShiftStatus(shift.id, "CLOSED")
            val device = operations.device()
            if (device != null) {
                enqueueOutbox(
                    operations, device, shift.siteId, shift.id, "SHIFT_CLOSED", close.id,
                    OutboxPayloadBuilder.shiftClosed(close), close.approvedAtEpochMillis
                )
            }
            if (printTicket) {
                operations.insertPrintJob(PrintJobEntity(UUID.randomUUID().toString(), close.id, "PENDING", false, System.currentTimeMillis(), "SHIFT_CLOSE"))
            }
            true
        }
        if (closed) true
        else throw IllegalStateException("No se pudo sellar el corte: el turno no está activo o los totales del turno no cuadran.")
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
            val cancelledAt = System.currentTimeMillis()
            val eventId = UUID.randomUUID().toString()
            operations.markSaleCancelled(sale.id)
            val cancellation = SaleCancellationEntity(UUID.randomUUID().toString(), sale.id, sale.shiftId, reason.trim(), manager.id, manager.role, cancelledAt)
            operations.insertCancellation(cancellation)
            operations.insertMovements(
                lines.filter { it.stockPolicy == "CONTROLLED" && it.productId != null }
                    .map { line -> InventoryMovementEntity(UUID.randomUUID().toString(), sale.id, line.productId!!, line.quantity, cancelledAt, "SALE_CANCELLATION", eventId) }
            )
            enqueueOutbox(
                operations, device, liveShift.siteId, liveShift.id, "SALE_CANCELLED", sale.id,
                OutboxPayloadBuilder.saleCancelled(sale, cancellation), cancelledAt, eventId
            )
            true
        }
    }

    fun confirmSale(shift: ShiftEntity, lines: List<CartLine>, method: PaymentMethod, tenderedCentavos: Long, discount: SaleDiscountDraft? = null): Result<SaleEntity> {
        if (lines.isEmpty()) return Result.failure(IllegalArgumentException("El carrito está vacío."))
        if (lines.any { it.quantity <= 0 || it.unitPriceCentavos <= 0 }) {
            return Result.failure(IllegalArgumentException("Hay una línea con cantidad o precio inválido."))
        }
        val policy = database.syncProjectionDao().policy()
        val allowedOpenCategories = openAmountCategories().toSet()
        lines.forEach { line ->
            when (line.lineType) {
                SaleLineType.CATALOG -> {
                    val product = line.productId?.let { database.productDao().findById(it) }
                        ?: return Result.failure(IllegalArgumentException("No se encontró ${line.name} en el catálogo local."))
                    if (!product.available) {
                        return Result.failure(IllegalArgumentException("${product.name} no está disponible."))
                    }
                    if (product.stockPolicy == "CONTROLLED" && policy?.allowNegativeStock != true) {
                        val stock = product.stock.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        if (stock < BigDecimal.valueOf(line.quantity.toLong())) {
                            return Result.failure(IllegalArgumentException("No hay existencias suficientes de ${product.name}."))
                        }
                    }
                }
                SaleLineType.PENDING_CATALOG -> Unit
                SaleLineType.OPEN_AMOUNT -> {
                    val authorizer = line.authorizedByUserId?.let { database.userDao().find(it) }
                    when {
                        !allowOpenProducts() ->
                            return Result.failure(IllegalStateException("Monto abierto no está habilitado en esta sede."))
                        line.quantity != 1 || line.unitPriceCentavos <= 0 ->
                            return Result.failure(IllegalArgumentException("Cada monto abierto debe ser una línea de cantidad 1."))
                        line.category.trim() !in allowedOpenCategories ->
                            return Result.failure(IllegalArgumentException("La categoría ${line.category} no está permitida para monto abierto."))
                        line.productId != null || !line.sourceBarcode.isNullOrBlank() || line.stockPolicy != "NOT_CONTROLLED" ->
                            return Result.failure(IllegalArgumentException("La línea de monto abierto no es válida."))
                        line.name != "Producto abierto · ${line.category.trim()}" ->
                            return Result.failure(IllegalArgumentException("La descripción de monto abierto no coincide con la categoría."))
                        line.authorizedAtEpochMillis == null || authorizer == null || !authorizer.active || !authorizer.isManagerOrAdmin ->
                            return Result.failure(IllegalArgumentException("El monto abierto requiere autorización de Manager o Superadmin."))
                    }
                }
            }
        }
        val gross = SaleCalculator.grossCentavos(lines)
        if (discount != null && (discount.amountCentavos <= 0 || discount.amountCentavos > gross || discount.reason.isBlank() || (discount.authorizedBy.role != "MANAGER" && discount.authorizedBy.role != "SUPERADMIN"))) {
            return Result.failure(IllegalArgumentException("El descuento no es válido."))
        }
        val total = SaleCalculator.netCentavos(gross, discount?.amountCentavos ?: 0)
        if (total == 0L && (method != PaymentMethod.CORTESIA || !SaleCalculator.isFullCourtesy(gross, discount?.amountCentavos ?: 0))) {
            return Result.failure(IllegalArgumentException("Una venta en cero debe registrarse como cortesía."))
        }
        if (total > 0L && method == PaymentMethod.CORTESIA) {
            return Result.failure(IllegalArgumentException("La cortesía solo aplica cuando el descuento cubre el total."))
        }
        if (method == PaymentMethod.CASH && tenderedCentavos < total) {
            return Result.failure(IllegalArgumentException("El efectivo recibido es menor al total."))
        }
        val sale = database.runInTransaction<SaleEntity?> {
            val operations = database.operationsDao()
            val liveShift = operations.activeShift() ?: return@runInTransaction null
            if (liveShift.id != shift.id) return@runInTransaction null
            val device = operations.device() ?: return@runInTransaction null
            val saleId = UUID.randomUUID().toString()
            val eventId = UUID.randomUUID().toString()
            val confirmedAt = System.currentTimeMillis()
            val change = if (method == PaymentMethod.CASH) SaleCalculator.changeCentavos(tenderedCentavos, total) else 0
            val folio = "${device.visibleCode}-${liveShift.id.take(4).uppercase()}-${liveShift.nextFolioNumber.toString().padStart(4, '0')}"
            val sale = SaleEntity(saleId, folio, liveShift.id, liveShift.cashierId, gross, discount?.amountCentavos ?: 0, total, method.name, if (method == PaymentMethod.CASH) tenderedCentavos else total, change, confirmedAt)
            operations.insertSale(sale)
            val discountEntity = discount?.let {
                SaleDiscountEntity(UUID.randomUUID().toString(), saleId, it.amountCentavos, it.reason.trim(), it.authorizedBy.id, it.authorizedBy.role, confirmedAt)
            }
            discountEntity?.let { operations.insertDiscount(it) }
            val saleLines = lines.map { line ->
                SaleLineEntity(
                    UUID.randomUUID().toString(),
                    saleId,
                    line.productId,
                    line.name,
                    line.category,
                    line.quantity,
                    line.unitPriceCentavos,
                    line.subtotalCentavos,
                    line.stockPolicy,
                    line.lineType.name,
                    line.sourceBarcode,
                    line.authorizedByUserId?.toLongOrNull() ?: line.authorizedByOperatorId,
                    line.authorizedAtEpochMillis,
                )
            }
            operations.insertLines(saleLines)
            val payment = PaymentEntity(UUID.randomUUID().toString(), saleId, method.name, total)
            operations.insertPayment(payment)
            val controlledLines = lines.filter { it.lineType == SaleLineType.CATALOG && it.stockPolicy == "CONTROLLED" && it.productId != null }
            operations.insertMovements(
                controlledLines.map { line ->
                    InventoryMovementEntity(UUID.randomUUID().toString(), saleId, line.productId!!, -line.quantity, confirmedAt, syncEventId = eventId)
                }
            )
            val products = lines.mapNotNull { line -> line.productId?.let { database.productDao().findById(it) } }.associateBy { it.id }
            enqueueOutbox(
                operations, device, liveShift.siteId, liveShift.id, "SALE_CONFIRMED", saleId,
                OutboxPayloadBuilder.saleConfirmed(sale, saleLines, payment, discountEntity, products), confirmedAt, eventId
            )
            operations.insertPrintJob(PrintJobEntity(UUID.randomUUID().toString(), saleId, "PENDING", false, confirmedAt))
            operations.updateFolioNumber(liveShift.id, liveShift.nextFolioNumber + 1)
            sale
        }
        return if (sale != null) Result.success(sale)
        else Result.failure(IllegalStateException("El turno ya no está activo en esta tablet."))
    }
    // Persists one outbox row with envelope metadata and advances the device sequence.
    private fun enqueueOutbox(
        operations: OperationsDao,
        device: DeviceEntity,
        siteId: String,
        shiftId: String,
        type: String,
        aggregateId: String,
        payloadJson: String,
        occurredAt: Long,
        eventId: String = UUID.randomUUID().toString(),
    ) {
        operations.insertOutbox(
            OutboxEventEntity(
                eventId, device.nextEventSequence, type, aggregateId, "PENDING", occurredAt,
                schemaVersion = 1, deviceId = device.id, siteId = siteId, shiftId = shiftId,
                occurredAtEpochMillis = occurredAt, payloadJson = payloadJson
            )
        )
        operations.updateSequence(device.id, device.nextEventSequence + 1)
    }
}

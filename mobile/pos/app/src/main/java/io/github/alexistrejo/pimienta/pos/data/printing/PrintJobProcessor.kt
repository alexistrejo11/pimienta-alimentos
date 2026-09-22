package io.github.alexistrejo.pimienta.pos.data.printing

import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.local.entity.PrintJobEntity
import io.github.alexistrejo.pimienta.pos.hardware.EscPosEncoder
import io.github.alexistrejo.pimienta.pos.hardware.PrintFailure
import io.github.alexistrejo.pimienta.pos.hardware.PrintResult
import io.github.alexistrejo.pimienta.pos.hardware.PrintableLine
import io.github.alexistrejo.pimienta.pos.hardware.TicketDocument
import io.github.alexistrejo.pimienta.pos.hardware.TicketPrinter
import io.github.alexistrejo.pimienta.pos.hardware.OperationalDocument
import io.github.alexistrejo.pimienta.pos.hardware.spanishPaymentLabel
import java.time.Instant

// Processes one durable print job without changing the sale or payment records.
class PrintJobProcessor(
    private val database: PosDatabase,
    private val printer: TicketPrinter,
    private val encoder: EscPosEncoder = EscPosEncoder(printer.profile),
) {
    // Claims and processes the oldest pending job, returning whether work existed.
    suspend fun processNext(now: Long = System.currentTimeMillis()): Boolean {
        val dao = database.operationsDao()
        val job = dao.nextPrintJob() ?: return false

        // Automatic print jobs older than 30 minutes (or from closed shifts) expire automatically to avoid wasting paper.
        if (shouldExpire(job, dao, now)) {
            dao.finishPrintJob(job.id, "EXPIRED", "EXPIRED_STALE_JOB")
            return true
        }

        if (dao.markPrintJobPrinting(job.id, now) == 0) return true
        val result = runCatching { print(job) }.getOrElse { PrintResult.Failed(PrintFailure.TRANSPORT_ERROR) }
        when (result) {
            PrintResult.Printed -> dao.finishPrintJob(job.id, "PRINTED", null)
            is PrintResult.Failed -> dao.finishPrintJob(job.id, "FAILED", result.reason.name)
        }
        return true
    }

    companion object {
        // Automatic print jobs older than 30 minutes expire automatically to avoid paper waste when reconnecting.
        const val STALE_PRINT_JOB_MAX_AGE_MILLIS = 30 * 60 * 1000L // 30 minutes

        // Determines if an automatic ticket job is stale and should be expired without printing.
        internal fun shouldExpire(
            job: PrintJobEntity,
            dao: io.github.alexistrejo.pimienta.pos.data.local.dao.OperationsDao,
            now: Long,
            maxAgeMillis: Long = STALE_PRINT_JOB_MAX_AGE_MILLIS,
        ): Boolean {
            // Manual reprint requests (duplicate = true) are explicitly requested by the user and never auto-expire.
            if (job.duplicate) return false

            // Expire if the job is older than the max age threshold (default 30 minutes).
            val ageMillis = now - job.createdAtEpochMillis
            if (ageMillis > maxAgeMillis) return true

            // Expire ticket print jobs if the shift in which the sale occurred is already CLOSED.
            if (job.documentType == "SALE") {
                val sale = dao.sale(job.saleId)
                if (sale != null) {
                    val shift = dao.findShift(sale.shiftId)
                    if (shift?.status == "CLOSED") return true
                }
            }

            return false
        }
    }

    // Builds the current sale snapshot and sends it through the configured printer.
    private suspend fun print(job: PrintJobEntity): PrintResult {
        val dao = database.operationsDao()
        val openDrawer = when (job.documentType) {
            "SALE" -> dao.sale(job.saleId)?.paymentMethod == "CASH"
            else -> false
        }
        val document = when (job.documentType) {
            "SALE" -> {
                val sale = dao.sale(job.saleId) ?: return PrintResult.Failed(PrintFailure.UNSUPPORTED)
                val site = database.siteDao().current()
                val isCash = sale.paymentMethod == "CASH"
                TicketDocument(
                    title = "Ticket de compra",
                    folio = sale.folio,
                    occurredAt = Instant.ofEpochMilli(sale.confirmedAtEpochMillis),
                    lines = dao.linesForSale(sale.id).map { PrintableLine(it.productName, it.quantity.toString(), it.subtotalCentavos) },
                    totalCentavos = sale.totalCentavos,
                    paymentLabel = spanishPaymentLabel(sale.paymentMethod),
                    duplicate = job.duplicate,
                    siteName = site?.name,
                    siteAddress = site?.address,
                    discountCentavos = sale.discountCentavos,
                    tenderedCentavos = if (isCash) sale.tenderedCentavos else null,
                    changeCentavos = if (isCash) sale.changeCentavos else null,
                )
            }
            "CASH_WITHDRAWAL" -> {
                val withdrawal = dao.withdrawal(job.saleId) ?: return PrintResult.Failed(PrintFailure.UNSUPPORTED)
                OperationalDocument("Retiro de efectivo", withdrawal.folio, Instant.ofEpochMilli(withdrawal.createdAtEpochMillis), listOf(PrintableLine(withdrawal.reason, "1", withdrawal.amountCentavos)), withdrawal.amountCentavos)
            }
            "SHIFT_CLOSE" -> {
                val close = dao.shiftClose(job.saleId) ?: return PrintResult.Failed(PrintFailure.UNSUPPORTED)
                val shift = dao.findShift(close.shiftId)
                val cashier = shift?.cashierId?.let { dao.findUser(it)?.displayName } ?: shift?.cashierId ?: "Cajero"
                val manager = dao.findUser(close.approvedByUserId)?.displayName ?: close.approvedByUserId
                val shiftId = close.shiftId
                val gross = dao.grossForShift(shiftId)
                val discounts = dao.discountsForShift(shiftId)
                val net = dao.netForShift(shiftId)
                val courtesy = dao.courtesyForShift(shiftId)
                val cash = dao.cashSalesForShift(shiftId)
                val card = dao.cardSalesForShift(shiftId)
                val ticketCount = dao.ticketCountForShift(shiftId)
                val cancelledCount = dao.cancelledCountForShift(shiftId)
                val cancelledCash = dao.cancelledCashForShift(shiftId)
                val withdrawals = dao.withdrawals(shiftId)
                val withdrawalTotal = dao.withdrawalsForShift(shiftId)
                val openAmount = dao.lineAmountForShift(shiftId, "OPEN_AMOUNT")
                val pendingCatalog = dao.lineAmountForShift(shiftId, "PENDING_CATALOG")
                val lines = dao.linesForShift(shiftId)

                val docLines = mutableListOf<PrintableLine>()
                docLines.add(PrintableLine("Cajero: $cashier"))
                docLines.add(PrintableLine("Autorizo: $manager"))
                if (shift != null) {
                    val openedStr = formatTime(shift.openedAtEpochMillis)
                    val closedStr = formatTime(close.approvedAtEpochMillis)
                    docLines.add(PrintableLine("Horario: $openedStr - $closedStr"))
                    docLines.add(PrintableLine("Fondo inicial", "1", shift.openingCashCentavos))
                }
                docLines.add(PrintableLine("Ventas brutas ($ticketCount tks)", "", gross))
                if (discounts > 0) docLines.add(PrintableLine("Descuentos", "", discounts))
                docLines.add(PrintableLine("Ventas netas", "", net))
                docLines.add(PrintableLine("Efectivo", "", cash))
                if (card > 0) docLines.add(PrintableLine("Tarjeta", "", card))
                if (courtesy > 0) docLines.add(PrintableLine("Cortesias", "", courtesy))
                if (openAmount > 0) docLines.add(PrintableLine("Monto abierto", "", openAmount))
                if (pendingCatalog > 0) docLines.add(PrintableLine("Sin catalogar", "", pendingCatalog))
                if (cancelledCount > 0) docLines.add(PrintableLine("Canceladas ($cancelledCount)", "", cancelledCash))
                if (withdrawals.isNotEmpty()) docLines.add(PrintableLine("Sangrias (${withdrawals.size})", "", withdrawalTotal))
                docLines.add(PrintableLine("Efectivo esperado", "1", close.expectedCashCentavos))
                docLines.add(PrintableLine("Efectivo contado", "1", close.countedCashCentavos))
                docLines.add(PrintableLine("Diferencia", "1", close.differenceCentavos))

                val productSummary = lines.groupBy { it.productName }.map { (name, items) ->
                    val qty = items.sumOf { it.quantity }
                    val subtotal = items.sumOf { it.subtotalCentavos }
                    PrintableLine(name, "$qty", subtotal)
                }.sortedByDescending { it.amountCentavos ?: 0L }

                if (productSummary.isNotEmpty()) {
                    docLines.add(PrintableLine("--- PRODUCTOS VENDIDOS ---"))
                    docLines.addAll(productSummary)
                }

                OperationalDocument(
                    title = "Corte de Caja",
                    folio = close.shiftId.take(8).uppercase(),
                    occurredAt = Instant.ofEpochMilli(close.approvedAtEpochMillis),
                    lines = docLines,
                    totalCentavos = close.countedCashCentavos,
                )
            }
            else -> return PrintResult.Failed(PrintFailure.UNSUPPORTED)
        }
        return printer.print(encoder.encode(document, openDrawer = openDrawer))
    }

    private fun formatTime(epochMillis: Long): String {
        return try {
            val zone = java.time.ZoneId.systemDefault()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")
            java.time.Instant.ofEpochMilli(epochMillis).atZone(zone).format(formatter)
        } catch (_: Exception) {
            ""
        }
    }
}

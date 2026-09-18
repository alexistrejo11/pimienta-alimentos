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
        if (dao.markPrintJobPrinting(job.id, now) == 0) return true
        val result = runCatching { print(job) }.getOrElse { PrintResult.Failed(PrintFailure.TRANSPORT_ERROR) }
        when (result) {
            PrintResult.Printed -> dao.finishPrintJob(job.id, "PRINTED", null)
            is PrintResult.Failed -> dao.finishPrintJob(job.id, "FAILED", result.reason.name)
        }
        return true
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

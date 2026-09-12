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
                TicketDocument(
                    title = "Ticket de compra",
                    folio = sale.folio,
                    occurredAt = Instant.ofEpochMilli(sale.confirmedAtEpochMillis),
                    lines = dao.linesForSale(sale.id).map { PrintableLine(it.productName, it.quantity.toString(), it.subtotalCentavos) },
                    totalCentavos = sale.totalCentavos,
                    paymentLabel = sale.paymentMethod,
                    duplicate = job.duplicate,
                )
            }
            "CASH_WITHDRAWAL" -> {
                val withdrawal = dao.withdrawal(job.saleId) ?: return PrintResult.Failed(PrintFailure.UNSUPPORTED)
                OperationalDocument("Retiro de efectivo", withdrawal.folio, Instant.ofEpochMilli(withdrawal.createdAtEpochMillis), listOf(PrintableLine(withdrawal.reason, "1", withdrawal.amountCentavos)), withdrawal.amountCentavos)
            }
            "SHIFT_CLOSE" -> {
                val close = dao.shiftClose(job.saleId) ?: return PrintResult.Failed(PrintFailure.UNSUPPORTED)
                OperationalDocument("Corte Z", close.shiftId, Instant.ofEpochMilli(close.approvedAtEpochMillis), listOf(
                    PrintableLine("Efectivo esperado", "1", close.expectedCashCentavos),
                    PrintableLine("Efectivo contado", "1", close.countedCashCentavos),
                    PrintableLine("Diferencia", "1", close.differenceCentavos),
                ), close.countedCashCentavos)
            }
            else -> return PrintResult.Failed(PrintFailure.UNSUPPORTED)
        }
        return printer.print(encoder.encode(document, openDrawer = openDrawer))
    }
}

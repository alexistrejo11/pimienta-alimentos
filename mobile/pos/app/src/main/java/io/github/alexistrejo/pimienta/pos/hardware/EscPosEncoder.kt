package io.github.alexistrejo.pimienta.pos.hardware

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

// Encodes structured POS documents into protocol bytes without Android dependencies.
class EscPosEncoder(private val profile: PrinterProfile = PrinterProfiles.genericEscPos58) {
    private val charset = Charset.forName(profile.codePage.charsetName)

    // Produces one complete ticket suitable for a configured ESC/POS transport.
    fun encode(document: PrintableDocument, openDrawer: Boolean = false): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(0x1B, 0x40))
        output.write(byteArrayOf(0x1B, 0x74, profile.codePage.escPosValue.toByte()))
        writeLine(output, "PIMIENTA ALIMENTOS", centered = true, bold = true)
        writeLine(output, document.title, centered = true, bold = true)
        if (document is TicketDocument && document.duplicate) writeLine(output, "REIMPRESION", centered = true)
        writeLine(output, "Folio: ${folio(document)}")
        writeLine(output, "Fecha: ${document.occurredAt}")
        output.write("-".repeat(profile.paperWidth.columns).toByteArray(charset))
        output.write('\n'.code)
        document.lines.forEach { line ->
            val amount = formatAmount(line.amountCentavos)
            writeLine(output, "${line.quantity} ${line.label}".take(profile.paperWidth.columns - amount.length) + amount)
        }
        output.write("-".repeat(profile.paperWidth.columns).toByteArray(charset))
        output.write('\n'.code)
        writeLine(output, "TOTAL ${formatAmount(document.totalCentavos)}", bold = true)
        document.paymentLabel?.let { writeLine(output, "Pago: $it") }
        writeLine(output, "Si no te entregamos tu ticket, tu consumo es GRATIS", centered = true)
        output.write(byteArrayOf(0x1B, 0x64, 0x03))
        if (openDrawer && profile.supportsCashDrawer) {
            val pulse = profile.drawerPulse ?: DrawerPulse()
            output.write(byteArrayOf(0x1B, 0x70, 0x00, pulse.onTime.toByte(), pulse.offTime.toByte()))
        }
        if (profile.supportsCut) output.write(byteArrayOf(0x1D, 0x56, 0x00))
        return output.toByteArray()
    }

    private fun folio(document: PrintableDocument) = when (document) {
        is TicketDocument -> document.folio
        is OperationalDocument -> document.folio
    }

    private fun writeLine(output: ByteArrayOutputStream, text: String, centered: Boolean = false, bold: Boolean = false) {
        output.write(byteArrayOf(0x1B, 0x45, if (bold) 1 else 0))
        val value = if (centered) text.padStart((profile.paperWidth.columns + text.length) / 2).takeLast(profile.paperWidth.columns) else text
        output.write(value.toByteArray(charset))
        output.write('\n'.code)
    }

    private fun formatAmount(centavos: Long): String = "${centavos / 100}.${(centavos % 100).toString().padStart(2, '0')}"
}

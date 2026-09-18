package io.github.alexistrejo.pimienta.pos.hardware

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        writeLine(output, "Fecha: ${formatDate(document.occurredAt)}")
        if (document is TicketDocument) {
            document.siteName?.takeIf { it.isNotBlank() }?.let { writeLine(output, it, centered = true) }
            document.siteAddress?.takeIf { it.isNotBlank() }?.let { writeLine(output, it) }
        }
        output.write("-".repeat(profile.paperWidth.columns).toByteArray(charset))
        output.write('\n'.code)
        document.lines.forEach { line ->
            val amountStr = line.amountCentavos?.let { formatAmount(it) } ?: ""
            val qtyStr = if (line.quantity.isNotBlank()) "${line.quantity} " else ""
            val left = "$qtyStr${line.label}"
            if (amountStr.isBlank()) {
                writeLine(output, left.take(profile.paperWidth.columns))
            } else {
                val maxLeft = (profile.paperWidth.columns - amountStr.length - 1).coerceAtLeast(1)
                val leftTruncated = left.take(maxLeft).padEnd(maxLeft)
                writeLine(output, "$leftTruncated $amountStr")
            }
        }
        output.write("-".repeat(profile.paperWidth.columns).toByteArray(charset))
        output.write('\n'.code)
        if (document is TicketDocument && document.discountCentavos > 0) {
            writeLine(output, "Descuento -${formatAmount(document.discountCentavos)}")
        }
        if (document.totalCentavos > 0 || document is TicketDocument) {
            writeLine(output, "TOTAL ${formatAmount(document.totalCentavos)}", bold = true)
        }
        document.paymentLabel?.let { writeLine(output, "Pago: $it") }
        if (document is TicketDocument && document.tenderedCentavos != null) {
            writeLine(output, "Recibido ${formatAmount(document.tenderedCentavos)}")
            document.changeCentavos?.let { writeLine(output, "Cambio ${formatAmount(it)}") }
        }
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

    private fun formatDate(instant: Instant): String {
        return try {
            val zone = ZoneId.systemDefault()
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            instant.atZone(zone).format(formatter)
        } catch (e: Exception) {
            instant.toString().take(19).replace('T', ' ')
        }
    }

    private fun writeLine(output: ByteArrayOutputStream, text: String, centered: Boolean = false, bold: Boolean = false) {
        output.write(byteArrayOf(0x1B, 0x45, if (bold) 1 else 0))
        val columns = profile.paperWidth.columns
        if (text.length <= columns) {
            val value = if (centered) text.padStart((columns + text.length) / 2).padEnd(columns) else text
            output.write(value.toByteArray(charset))
            output.write('\n'.code)
        } else {
            text.chunked(columns).forEach { chunk ->
                val value = if (centered) chunk.padStart((columns + chunk.length) / 2).padEnd(columns) else chunk
                output.write(value.toByteArray(charset))
                output.write('\n'.code)
            }
        }
    }

    private fun formatAmount(centavos: Long): String = "${centavos / 100}.${(centavos % 100).toString().padStart(2, '0')}"
}

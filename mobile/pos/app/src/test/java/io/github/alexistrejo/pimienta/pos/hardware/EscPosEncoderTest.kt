package io.github.alexistrejo.pimienta.pos.hardware

import java.time.Instant
import java.nio.charset.Charset
import org.junit.Assert.assertTrue
import org.junit.Test

// Verifies that the generic 58 mm encoder emits protocol markers and Spanish text.
class EscPosEncoderTest {
    @Test
    fun encodes_ticket_with_spanish_text_and_cut() {
        val document = TicketDocument(
            title = "Ticket de compra",
            folio = "T1-001-0001",
            occurredAt = Instant.parse("2026-09-10T12:00:00Z"),
            lines = listOf(PrintableLine("Jugo de piña", "1", 1250)),
            totalCentavos = 1250,
            paymentLabel = "CASH",
        )

        val bytes = EscPosEncoder().encode(document)
        val text = bytes.toString(Charset.forName("CP850"))

        assertTrue(bytes.take(3).toByteArray().contentEquals(byteArrayOf(0x1B, 0x40, 0x1B)))
        assertTrue(text.contains("Jugo de piña"))
        assertTrue(bytes.toList().contains(0x1D.toByte()))
    }
}

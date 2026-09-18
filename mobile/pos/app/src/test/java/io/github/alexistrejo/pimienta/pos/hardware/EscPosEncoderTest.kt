package io.github.alexistrejo.pimienta.pos.hardware

import java.nio.charset.Charset
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Verifies that the generic 58 mm encoder emits protocol markers and Spanish text.
class EscPosEncoderTest {
    @Test
    fun encodes_ticket_with_spanish_text_cut_and_drawer_pulse() {
        val document = TicketDocument(
            title = "Ticket de compra",
            folio = "T1-001-0001",
            occurredAt = Instant.parse("2026-09-10T12:00:00Z"),
            lines = listOf(
                PrintableLine("Jugo de piña", "1", 1250),
                PrintableLine("Testing Dummy Product", "2", 3000)
            ),
            totalCentavos = 4150,
            paymentLabel = "Efectivo",
            siteName = "Comedor Centro",
            siteAddress = "Av. Principal 123",
            discountCentavos = 100,
            tenderedCentavos = 5000,
            changeCentavos = 850,
        )

        val profile = PrinterProfiles.pos5890A
        val bytes = EscPosEncoder(profile).encode(document, openDrawer = true)
        val text = bytes.toString(Charset.forName("CP850"))

        assertTrue(bytes.take(3).toByteArray().contentEquals(byteArrayOf(0x1B, 0x40, 0x1B)))
        assertTrue(text.contains("Jugo de piña"))
        assertTrue(text.contains("Testing Dummy Product"))
        assertTrue(text.contains("Comedor Centro"))
        assertTrue(text.contains("Descuento"))
        assertTrue(text.contains("Pago: Efectivo"))
        assertTrue(text.contains("Recibido"))
        assertTrue(text.contains("Cambio"))
        assertTrue(bytes.toList().contains(0x1D.toByte()))
        assertTrue(bytes.toList().contains(0x70.toByte()))
    }

    @Test
    fun operational_document_omits_promotional_footer() {
        val document = OperationalDocument(
            title = "Retiro de efectivo",
            folio = "W-001",
            occurredAt = Instant.parse("2026-09-10T14:00:00Z"),
            lines = listOf(PrintableLine("Resguardo de caja", "1", 50000)),
            totalCentavos = 50000,
        )

        val bytes = EscPosEncoder(PrinterProfiles.pos5890A).encode(document)
        val text = bytes.toString(Charset.forName("CP850"))

        assertTrue(text.contains("Retiro de efectivo"))
        assertTrue(text.contains("Resguardo de caja"))
        assertFalse(text.contains("Si no te entregamos tu ticket, tu consumo es GRATIS"))
    }

    @Test
    fun encodes_shift_close_summary_ticket_with_product_breakdown() {
        val document = OperationalDocument(
            title = "Corte de Caja",
            folio = "SHIFT-01",
            occurredAt = Instant.parse("2026-09-10T18:00:00Z"),
            lines = listOf(
                PrintableLine("Cajero: Juan Perez"),
                PrintableLine("Autorizo: Maria Manager"),
                PrintableLine("Horario: 10/09 08:00 - 10/09 18:00"),
                PrintableLine("Fondo inicial", "1", 50000),
                PrintableLine("Ventas brutas (12 tks)", "", 150000),
                PrintableLine("Descuentos", "", 10000),
                PrintableLine("Ventas netas", "", 140000),
                PrintableLine("Cortesias", "", 2000),
                PrintableLine("Sangrias (2)", "", 30000),
                PrintableLine("Efectivo esperado", "1", 160000),
                PrintableLine("Efectivo contado", "1", 160000),
                PrintableLine("Diferencia", "1", 0),
                PrintableLine("--- PRODUCTOS VENDIDOS ---"),
                PrintableLine("Boing de Mango", "10", 25000),
                PrintableLine("Pan Dulce", "5", 7500),
            ),
            totalCentavos = 160000,
        )

        val bytes = EscPosEncoder(PrinterProfiles.pos5890A).encode(document)
        val text = bytes.toString(Charset.forName("CP850"))

        assertTrue(text.contains("Corte de Caja"))
        assertTrue(text.contains("Cajero: Juan Perez"))
        assertTrue(text.contains("Autorizo: Maria Manager"))
        assertTrue(text.contains("Fondo inicial"))
        assertTrue(text.contains("Ventas brutas"))
        assertTrue(text.contains("Sangrias"))
        assertTrue(text.contains("Efectivo esperado"))
        assertTrue(text.contains("Efectivo contado"))
        assertTrue(text.contains("--- PRODUCTOS VENDIDOS ---"))
        assertTrue(text.contains("Boing de Mango"))
        assertTrue(text.contains("Pan Dulce"))
        assertFalse(text.contains("Si no te entregamos tu ticket, tu consumo es GRATIS"))
    }
}

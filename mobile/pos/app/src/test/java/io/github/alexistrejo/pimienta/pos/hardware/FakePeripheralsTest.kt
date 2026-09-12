package io.github.alexistrejo.pimienta.pos.hardware

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

// Verifies deterministic fake peripherals used by UI and domain tests.
class FakePeripheralsTest {
    @Test
    fun scanner_emits_normalized_fake_reading() = runBlocking {
        val scanner = FakeBarcodeScanner()
        scanner.emit("  750123  ")

        val reading = scanner.events.first()
        assertEquals("750123", reading.rawValue)
        assertEquals(ScannerSource.FAKE, reading.source)
    }

    @Test
    fun printer_can_simulate_paper_failure() = runBlocking {
        val printer = FakeTicketPrinter()
        printer.nextResult = PrintResult.Failed(PrintFailure.PAPER_EMPTY)

        assertEquals(PrintResult.Failed(PrintFailure.PAPER_EMPTY), printer.print(byteArrayOf(1)))
    }
}

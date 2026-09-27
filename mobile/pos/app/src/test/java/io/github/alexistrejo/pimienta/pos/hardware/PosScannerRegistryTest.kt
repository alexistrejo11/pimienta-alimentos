package io.github.alexistrejo.pimienta.pos.hardware

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Last HID/fake read is presence, not a toggle.
class PosScannerRegistryTest {
    @Test
    fun recentReadExpiresAfterTheWindow() {
        PosScannerRegistry.noteRead("750123")
        val at = PosScannerRegistry.lastReadAtMillis
        assertTrue(PosScannerRegistry.recentRead(nowMillis = at + 1_000, windowMs = 120_000))
        assertFalse(PosScannerRegistry.recentRead(nowMillis = at + 121_000, windowMs = 120_000))
    }
}

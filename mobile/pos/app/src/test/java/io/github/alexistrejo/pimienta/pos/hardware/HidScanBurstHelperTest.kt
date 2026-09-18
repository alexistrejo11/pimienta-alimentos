package io.github.alexistrejo.pimienta.pos.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Verifies wedge scanner character accumulation without Android key events.
class HidScanBurstHelperTest {
    @Test
    fun accumulates_scanner_characters_and_emits_barcode_on_enter() {
        val helper = HidScanBurstHelper()
        assertTrue(helper.onCharacter('7'))
        assertTrue(helper.onCharacter('5'))
        assertTrue(helper.onCharacter('0'))
        assertTrue(helper.onCharacter('1'))
        assertEquals("7501", helper.onEnter())
    }

    @Test
    fun empty_scan_returns_null() {
        val helper = HidScanBurstHelper()
        assertNull(helper.onEnter())
    }

    @Test
    fun ean13_and_16_char_long_barcodes_are_accepted_completely() {
        val helper = HidScanBurstHelper()
        val ean13 = "7501073839854" // 13 digits EAN-13
        
        ean13.forEach { char ->
            assertTrue(helper.onCharacter(char))
        }
        assertEquals("7501073839854", helper.onEnter())

        val code16 = "CAF-0033-2-ABC16" // 16 characters barcode
        code16.forEach { char ->
            assertTrue(helper.onCharacter(char))
        }
        assertEquals("CAF-0033-2-ABC16", helper.onEnter())
    }
}

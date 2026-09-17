package io.github.alexistrejo.pimienta.pos.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Verifies wedge burst timing without Android key events.
class HidScanBurstHelperTest {
    @Test
    fun slow_typing_is_not_consumed_as_scan() {
        val helper = HidScanBurstHelper()
        assertFalse(helper.onCharacter('7', 0))
        assertFalse(helper.onCharacter('5', 200))
        assertNull(helper.onEnter())
    }

    @Test
    fun fast_burst_with_enter_emits_barcode() {
        val helper = HidScanBurstHelper()
        assertFalse(helper.onCharacter('7', 0))
        assertTrue(helper.onCharacter('5', 20))
        assertTrue(helper.onCharacter('0', 40))
        assertTrue(helper.onCharacter('1', 60))
        assertEquals("7501", helper.onEnter())
    }

    @Test
    fun short_burst_is_rejected() {
        val helper = HidScanBurstHelper()
        assertFalse(helper.onCharacter('1', 0))
        assertTrue(helper.onCharacter('2', 20))
        assertTrue(helper.onCharacter('3', 40))
        assertNull(helper.onEnter())
    }

    @Test
    fun accepts_inter_character_delays_up_to_max_gap() {
        val helper = HidScanBurstHelper(maxGapMs = 150L)
        assertFalse(helper.onCharacter('7', 0))
        assertTrue(helper.onCharacter('5', 100))
        assertTrue(helper.onCharacter('0', 220))
        assertTrue(helper.onCharacter('1', 330))
        assertEquals("7501", helper.onEnter())
    }

    @Test
    fun ean13_and_16_char_long_barcodes_are_accepted_completely() {
        val helper = HidScanBurstHelper(maxGapMs = 150L)
        val ean13 = "7501073839854" // 13 digits EAN-13
        
        ean13.forEachIndexed { index, char ->
            val now = index * 40L // 40ms per key delay
            if (index == 0) {
                assertFalse(helper.onCharacter(char, now))
            } else {
                assertTrue(helper.onCharacter(char, now))
            }
        }
        assertEquals("7501073839854", helper.onEnter())

        val code16 = "CAF-0033-2-ABC16" // 16 characters barcode
        code16.forEachIndexed { index, char ->
            val now = index * 60L // 60ms per key delay
            if (index == 0) {
                assertFalse(helper.onCharacter(char, now))
            } else {
                assertTrue(helper.onCharacter(char, now))
            }
        }
        assertEquals("CAF-0033-2-ABC16", helper.onEnter())
    }
}

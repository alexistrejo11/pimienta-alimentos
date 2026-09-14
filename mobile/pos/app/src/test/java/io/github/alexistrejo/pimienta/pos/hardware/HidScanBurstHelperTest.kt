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
}

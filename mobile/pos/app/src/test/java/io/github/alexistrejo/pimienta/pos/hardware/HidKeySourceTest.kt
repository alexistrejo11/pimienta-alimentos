package io.github.alexistrejo.pimienta.pos.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HidKeySourceTest {
    @Test
    fun ignoresVirtualAndSoftKeyboards() {
        assertTrue(HidKeySource.shouldIgnoreHumanKeyboard(deviceId = -1, flags = 0, isVirtualDevice = true, source = 0x101))
        assertTrue(
            HidKeySource.shouldIgnoreHumanKeyboard(
                deviceId = 3,
                flags = HidKeySource.FLAG_SOFT_KEYBOARD,
                isVirtualDevice = false,
                source = 0x101,
            ),
        )
        assertTrue(HidKeySource.shouldIgnoreHumanKeyboard(deviceId = 3, flags = 0, isVirtualDevice = true, source = 0x101))
        assertTrue(
            HidKeySource.shouldIgnoreHumanKeyboard(
                deviceId = 3,
                flags = 0,
                isVirtualDevice = false,
                source = HidKeySource.SOURCE_TOUCHSCREEN,
            ),
        )
    }

    @Test
    fun acceptsExternalHidKeyboardWedge() {
        assertFalse(
            HidKeySource.shouldIgnoreHumanKeyboard(
                deviceId = 7,
                flags = 0,
                isVirtualDevice = false,
                source = 0x101,
            ),
        )
    }

    @Test
    fun barcodeCharUsesDigitKeycodesWhenUnicodeIsZero() {
        assertEquals('5', HidKeySource.barcodeChar(HidKeySource.KEYCODE_0 + 5, unicodeChar = 0))
        assertEquals('7', HidKeySource.barcodeChar(HidKeySource.KEYCODE_NUMPAD_0 + 7, unicodeChar = 0))
        assertEquals('A', HidKeySource.barcodeChar(29, unicodeChar = 'A'.code))
        assertTrue(HidKeySource.isEnterKeyCode(HidKeySource.KEYCODE_ENTER))
        assertTrue(HidKeySource.isEnterKeyCode(HidKeySource.KEYCODE_NUMPAD_ENTER))
        assertFalse(HidKeySource.isEnterKeyCode(HidKeySource.KEYCODE_0))
    }
}

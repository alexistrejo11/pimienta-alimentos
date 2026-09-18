package io.github.alexistrejo.pimienta.pos.hardware

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
}

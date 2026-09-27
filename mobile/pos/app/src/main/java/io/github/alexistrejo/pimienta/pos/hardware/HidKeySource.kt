package io.github.alexistrejo.pimienta.pos.hardware

// Decides whether a key event came from on-screen typing instead of a USB/Bluetooth wedge scanner.
internal object HidKeySource {
    // Mirrors android.view.KeyEvent.FLAG_SOFT_KEYBOARD without needing a KeyEvent in unit tests.
    const val FLAG_SOFT_KEYBOARD = 0x2

    // Mirrors android.view.InputDevice.SOURCE_TOUCHSCREEN.
    const val SOURCE_TOUCHSCREEN = 0x00001000

    fun shouldIgnoreHumanKeyboard(
        deviceId: Int,
        flags: Int,
        isVirtualDevice: Boolean,
        source: Int,
    ): Boolean {
        if (deviceId < 0) return true
        if (flags and FLAG_SOFT_KEYBOARD != 0) return true
        if (isVirtualDevice) return true
        if (source and SOURCE_TOUCHSCREEN == SOURCE_TOUCHSCREEN) return true
        return false
    }

    // Mirrors android.view.KeyEvent so HID Enter and digits can be identified in unit tests.
    const val KEYCODE_ENTER = 66
    const val KEYCODE_NUMPAD_ENTER = 160
    const val KEYCODE_0 = 7
    const val KEYCODE_9 = 16
    const val KEYCODE_NUMPAD_0 = 144
    const val KEYCODE_NUMPAD_9 = 153

    fun isEnterKeyCode(keyCode: Int): Boolean =
        keyCode == KEYCODE_ENTER || keyCode == KEYCODE_NUMPAD_ENTER

    // Prefers unicode; falls back to digit keycodes when the wedge sends KEYCODE_0-9 with unicode 0.
    fun barcodeChar(keyCode: Int, unicodeChar: Int): Char? {
        if (unicodeChar != 0) {
            val char = unicodeChar.toChar()
            if (char.isLetterOrDigit() || char in "-._/") return char
        }
        return when (keyCode) {
            in KEYCODE_0..KEYCODE_9 -> ('0'.code + (keyCode - KEYCODE_0)).toChar()
            in KEYCODE_NUMPAD_0..KEYCODE_NUMPAD_9 -> ('0'.code + (keyCode - KEYCODE_NUMPAD_0)).toChar()
            else -> null
        }
    }
}

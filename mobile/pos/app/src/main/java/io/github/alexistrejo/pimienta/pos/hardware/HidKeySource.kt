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
}

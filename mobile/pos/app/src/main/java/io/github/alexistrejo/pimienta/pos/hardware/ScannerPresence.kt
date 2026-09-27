package io.github.alexistrejo.pimienta.pos.hardware

import android.view.InputDevice

// True when Android reports a non-virtual alphabetic HID keyboard (USB or BT wedge).
fun physicalHidKeyboardPresent(): Boolean {
    return InputDevice.getDeviceIds().any { id ->
        val device = InputDevice.getDevice(id) ?: return@any false
        if (device.isVirtual) return@any false
        val keyboard = device.sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD
        keyboard && device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
    }
}

fun scannerLooksPresent(bondedScanners: List<BondedBluetoothDevice>): Boolean {
    if (bondedScanners.isNotEmpty()) return true
    if (PosScannerRegistry.recentRead()) return true
    return physicalHidKeyboardPresent()
}

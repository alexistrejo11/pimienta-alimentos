package io.github.alexistrejo.pimienta.pos.hardware

// Decides when to show the USB printer permission dialog so hub enumeration cannot spam it.
class UsbPermissionGate {
    private val pendingDeviceIds = mutableSetOf<Int>()

    fun isPrinterCandidate(deviceClass: Int, interfaceClasses: List<Int>): Boolean {
        if (deviceClass == USB_CLASS_PRINTER) return true
        return interfaceClasses.any { it == USB_CLASS_PRINTER }
    }

    // True only for the first in-flight request per printer device.
    @Synchronized
    fun shouldPrompt(deviceId: Int, isPrinter: Boolean, hasPermission: Boolean): Boolean {
        if (!isPrinter || hasPermission) return false
        return pendingDeviceIds.add(deviceId)
    }

    @Synchronized
    fun markResolved(deviceId: Int) {
        pendingDeviceIds.remove(deviceId)
    }

    companion object {
        const val USB_CLASS_PRINTER = 7
    }
}

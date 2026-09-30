package io.github.alexistrejo.pimienta.pos.hardware

// Which physical link a print attempt will use. USB wins when a printer is on the bus.
enum class PrinterLink { USB, BLUETOOTH, NONE }

enum class PrinterRoute { USB, USB_PERMISSION, BLUETOOTH, NONE }

// Charging occupies the only USB-C port, so a saved Bluetooth printer is a normal path, not a degraded one.
// A USB printer still waiting for Android permission must not block a working Bluetooth printer.
fun choosePrinterRoute(
    usb: PeripheralStatus,
    savedMac: String?,
    bluetoothReady: Boolean = true,
): PrinterRoute {
    val hasBluetooth = !savedMac.isNullOrBlank()
    return when (usb) {
        PeripheralStatus.READY -> PrinterRoute.USB
        PeripheralStatus.PERMISSION_REQUIRED ->
            if (hasBluetooth && bluetoothReady) PrinterRoute.BLUETOOTH else PrinterRoute.USB_PERMISSION
        else -> if (hasBluetooth) PrinterRoute.BLUETOOTH else PrinterRoute.NONE
    }
}

fun printerLinkFor(route: PrinterRoute): PrinterLink = when (route) {
    PrinterRoute.USB, PrinterRoute.USB_PERMISSION -> PrinterLink.USB
    PrinterRoute.BLUETOOTH -> PrinterLink.BLUETOOTH
    PrinterRoute.NONE -> PrinterLink.NONE
}

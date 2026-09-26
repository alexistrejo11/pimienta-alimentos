package io.github.alexistrejo.pimienta.pos.hardware

// Which physical link a print attempt will use. USB wins when a printer is on the bus.
enum class PrinterLink { USB, BLUETOOTH, NONE }

enum class PrinterRoute { USB, USB_PERMISSION, BLUETOOTH, NONE }

// Charging occupies the only USB-C port, so a saved Bluetooth printer is a normal path, not a degraded one.
fun choosePrinterRoute(usb: PeripheralStatus, savedMac: String?): PrinterRoute = when (usb) {
    PeripheralStatus.READY -> PrinterRoute.USB
    PeripheralStatus.PERMISSION_REQUIRED -> PrinterRoute.USB_PERMISSION
    else -> if (savedMac.isNullOrBlank()) PrinterRoute.NONE else PrinterRoute.BLUETOOTH
}

fun printerLinkFor(route: PrinterRoute): PrinterLink = when (route) {
    PrinterRoute.USB, PrinterRoute.USB_PERMISSION -> PrinterLink.USB
    PrinterRoute.BLUETOOTH -> PrinterLink.BLUETOOTH
    PrinterRoute.NONE -> PrinterLink.NONE
}

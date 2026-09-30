package io.github.alexistrejo.pimienta.pos.hardware

// Colour of the status dot in Impresión y dispositivos: ready, usable with a caveat, or blocked.
enum class DeviceTone { OK, WARNING, BLOCKED }

// Follow-up the cashier can take from a status row. The panel maps each one to a button.
enum class DeviceFix { GRANT_USB, OPEN_BLUETOOTH_SETTINGS }

data class DeviceStatusLine(
    val title: String,
    val detail: String?,
    val tone: DeviceTone,
    val fix: DeviceFix? = null,
)

// Hardware facts the printer row needs. Kept free of Android types so the copy is unit-tested.
data class PrinterPanelInput(
    val usb: PeripheralStatus,
    val savedMac: String?,
    val savedPrinterName: String?,
    val bluetoothRadioReady: Boolean,
    val bluetoothStatus: PeripheralStatus,
    val training: Boolean,
)

// Answers "where does the next ticket go?" in words a cashier can act on.
fun printerStatusLine(input: PrinterPanelInput): DeviceStatusLine {
    val hasBluetooth = !input.savedMac.isNullOrBlank()
    val btName = input.savedPrinterName ?: "impresora Bluetooth"
    return when (choosePrinterRoute(input.usb, input.savedMac, input.bluetoothRadioReady)) {
        PrinterRoute.USB -> DeviceStatusLine(
            title = "Lista · por cable USB",
            detail = if (hasBluetooth) "Si desconectas el cable, se imprimirá por Bluetooth ($btName)." else null,
            tone = DeviceTone.OK,
        )
        PrinterRoute.USB_PERMISSION -> DeviceStatusLine(
            title = "Cable conectado · falta permiso",
            detail = "Toca Dar permiso USB y acepta el aviso de Android.",
            tone = DeviceTone.BLOCKED,
            fix = DeviceFix.GRANT_USB,
        )
        PrinterRoute.BLUETOOTH -> bluetoothLine(input, btName)
        PrinterRoute.NONE -> DeviceStatusLine(
            title = if (input.training) "Sin impresora · modo entrenamiento" else "Sin impresora",
            detail = "Conecta la impresora por cable o elige una impresora Bluetooth abajo.",
            tone = if (input.training) DeviceTone.WARNING else DeviceTone.BLOCKED,
        )
    }
}

private fun bluetoothLine(input: PrinterPanelInput, btName: String): DeviceStatusLine {
    // Only reached with USB pending when the radio is on; see choosePrinterRoute.
    if (input.usb == PeripheralStatus.PERMISSION_REQUIRED) {
        return DeviceStatusLine(
            title = "Imprimiendo por Bluetooth · $btName",
            detail = "El cable está conectado pero sin permiso. Toca Dar permiso USB para usarlo.",
            tone = DeviceTone.WARNING,
            fix = DeviceFix.GRANT_USB,
        )
    }
    if (!input.bluetoothRadioReady) {
        return DeviceStatusLine(
            title = "Bluetooth apagado",
            detail = "Enciende Bluetooth en la tablet o conecta la impresora por cable.",
            tone = DeviceTone.BLOCKED,
            fix = DeviceFix.OPEN_BLUETOOTH_SETTINGS,
        )
    }
    return when (input.bluetoothStatus) {
        PeripheralStatus.READY, PeripheralStatus.BUSY -> DeviceStatusLine(
            title = "Lista · por Bluetooth",
            detail = btName,
            tone = DeviceTone.OK,
        )
        PeripheralStatus.DISCOVERED -> DeviceStatusLine(
            title = "Encontrada · por Bluetooth",
            detail = "$btName se conecta al imprimir el primer ticket.",
            tone = DeviceTone.OK,
        )
        PeripheralStatus.ERROR -> DeviceStatusLine(
            title = "Bluetooth sin respuesta",
            detail = "Revisa que $btName esté encendida, con papel y cerca de la tablet.",
            tone = DeviceTone.BLOCKED,
        )
        else -> DeviceStatusLine(
            title = "En espera · por Bluetooth",
            detail = "$btName se conecta al imprimir. Usa Imprimir prueba para confirmar.",
            tone = DeviceTone.WARNING,
        )
    }
}

fun scannerStatusLine(present: Boolean): DeviceStatusLine = if (present) {
    DeviceStatusLine(title = "Detectado", detail = null, tone = DeviceTone.OK)
} else {
    DeviceStatusLine(
        title = "No detectado",
        detail = "Conecta el lector o emparéjalo en Ajustes de Android. Usa Probar lectura para confirmar.",
        tone = DeviceTone.WARNING,
    )
}

// Spanish outcome of the test ticket. Pass PrinterLink.NONE when the print went to the training fake.
fun testPrintMessage(result: PrintResult, link: PrinterLink): String = when (result) {
    is PrintResult.Printed -> when (link) {
        PrinterLink.USB -> "Prueba enviada por cable USB. Revisa el ticket y el cajón."
        PrinterLink.BLUETOOTH -> "Prueba enviada por Bluetooth. Revisa el ticket y el cajón."
        PrinterLink.NONE -> "Prueba simulada: no hay impresora conectada."
    }
    is PrintResult.Failed -> printFailureMessage(result.reason)
}

fun printFailureMessage(reason: PrintFailure): String = when (reason) {
    PrintFailure.NO_PRINTER -> "No hay impresora conectada. Conecta el cable o elige una impresora Bluetooth."
    PrintFailure.PERMISSION_REQUIRED -> "Falta dar permiso a la impresora. Toca Dar permiso USB o Permitir Bluetooth."
    PrintFailure.PAPER_EMPTY -> "La impresora no tiene papel. Cambia el rollo y vuelve a probar."
    PrintFailure.DISCONNECTED -> "La impresora se desconectó. Vuelve a probar."
    PrintFailure.TIMEOUT -> "La impresora no respondió. Revisa que esté encendida y cerca de la tablet."
    PrintFailure.UNSUPPORTED -> "Esta impresora no es compatible."
    PrintFailure.TRANSPORT_ERROR -> "No se pudo enviar a la impresora. Revisa la conexión y vuelve a probar."
}

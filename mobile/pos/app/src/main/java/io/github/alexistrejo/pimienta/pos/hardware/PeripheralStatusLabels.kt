package io.github.alexistrejo.pimienta.pos.hardware

import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode

// Maps runtime printer state to Spanish sale-floor labels and alert styling.
data class PrinterStatusPresentation(val label: String, val alert: Boolean)

fun printerStatusPresentation(mode: RuntimeMode, status: PeripheralStatus): PrinterStatusPresentation {
    return when (status) {
        PeripheralStatus.READY -> PrinterStatusPresentation("Impresora en línea", alert = false)
        PeripheralStatus.PERMISSION_REQUIRED -> PrinterStatusPresentation("Impresora: permiso USB", alert = true)
        else -> {
            if (mode == RuntimeMode.SANDBOX) {
                PrinterStatusPresentation("Impresora simulada", alert = false)
            } else {
                PrinterStatusPresentation("Impresora fuera de línea", alert = true)
            }
        }
    }
}

// Translates persisted payment codes into ticket-facing Spanish labels.
fun spanishPaymentLabel(method: String): String = when (method.uppercase()) {
    "CASH" -> "Efectivo"
    "CARD" -> "Tarjeta"
    "CORTESIA" -> "Cortesía"
    else -> method
}

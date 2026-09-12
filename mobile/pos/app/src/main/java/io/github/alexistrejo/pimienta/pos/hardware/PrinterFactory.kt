package io.github.alexistrejo.pimienta.pos.hardware

import android.content.Context
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Selects the printer implementation for the active runtime mode.
object PrinterFactory {
    fun create(context: Context, mode: RuntimeMode): TicketPrinter {
        return when (mode) {
            RuntimeMode.SANDBOX -> FakeTicketPrinter(profile = PrinterProfiles.pos5890A)
            RuntimeMode.PRODUCTION -> UsbTicketPrinter.open(context) ?: UnavailableTicketPrinter
        }
    }

    fun printerStatus(context: Context, mode: RuntimeMode): PeripheralStatus {
        return when (mode) {
            RuntimeMode.SANDBOX -> PeripheralStatus.READY
            RuntimeMode.PRODUCTION -> UsbTicketPrinter.status(context)
        }
    }
}

// Makes production failures explicit until a physical printer adapter is installed.
private object UnavailableTicketPrinter : TicketPrinter {
    override val profile: PrinterProfile = PrinterProfiles.pos5890A
    override val status: Flow<PeripheralStatus> = MutableStateFlow(PeripheralStatus.DISCONNECTED)
    override suspend fun print(bytes: ByteArray): PrintResult = PrintResult.Failed(PrintFailure.NO_PRINTER)
}

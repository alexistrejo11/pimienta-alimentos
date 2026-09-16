package io.github.alexistrejo.pimienta.pos.hardware

import android.content.Context
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Selects the printer implementation for the active runtime mode.
object PrinterFactory {
    fun create(context: Context, mode: RuntimeMode): TicketPrinter {
        val real = UsbTicketPrinter.open(context)
        if (real != null) return real

        val status = UsbTicketPrinter.status(context)
        if (status == PeripheralStatus.PERMISSION_REQUIRED) return PermissionPendingTicketPrinter

        return if (mode == RuntimeMode.SANDBOX) {
            FakeTicketPrinter(profile = PrinterProfiles.pos5890A)
        } else {
            UnavailableTicketPrinter
        }
    }

    fun printerStatus(context: Context, mode: RuntimeMode): PeripheralStatus {
        return UsbTicketPrinter.status(context)
    }
}

// Makes a missing USB printer explicit so the sale still queues a durable print job.
private object UnavailableTicketPrinter : TicketPrinter {
    override val profile: PrinterProfile = PrinterProfiles.pos5890A
    override val status: Flow<PeripheralStatus> = MutableStateFlow(PeripheralStatus.DISCONNECTED)
    override suspend fun print(bytes: ByteArray): PrintResult = PrintResult.Failed(PrintFailure.NO_PRINTER)
}

// Avoids treating a visible printer as missing while Android is still asking for USB permission.
private object PermissionPendingTicketPrinter : TicketPrinter {
    override val profile: PrinterProfile = PrinterProfiles.pos5890A
    override val status: Flow<PeripheralStatus> = MutableStateFlow(PeripheralStatus.PERMISSION_REQUIRED)
    override suspend fun print(bytes: ByteArray): PrintResult = PrintResult.Failed(PrintFailure.PERMISSION_REQUIRED)
}

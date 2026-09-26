package io.github.alexistrejo.pimienta.pos.hardware

import android.content.Context
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PrinterAvailability(val status: PeripheralStatus, val link: PrinterLink)

// Selects the printer implementation for the active runtime mode.
object PrinterFactory {
    fun create(context: Context, mode: RuntimeMode): TicketPrinter {
        val mac = PrinterPreferences(context).mac()
        return when (choosePrinterRoute(UsbTicketPrinter.status(context), mac)) {
            PrinterRoute.USB -> UsbTicketPrinter.open(context) ?: UnavailableTicketPrinter
            PrinterRoute.USB_PERMISSION -> PermissionPendingTicketPrinter
            PrinterRoute.BLUETOOTH -> {
                if (mac != null && bluetoothRadioReady(context)) {
                    BluetoothTicketPrinter(context, mac)
                } else if (mode == RuntimeMode.SANDBOX) {
                    FakeTicketPrinter(profile = PrinterProfiles.pos5890A)
                } else {
                    UnavailableTicketPrinter
                }
            }
            PrinterRoute.NONE -> if (mode == RuntimeMode.SANDBOX) {
                FakeTicketPrinter(profile = PrinterProfiles.pos5890A)
            } else {
                UnavailableTicketPrinter
            }
        }
    }

    fun availability(context: Context, @Suppress("UNUSED_PARAMETER") mode: RuntimeMode): PrinterAvailability {
        val mac = PrinterPreferences(context).mac()
        return when (val route = choosePrinterRoute(UsbTicketPrinter.status(context), mac)) {
            PrinterRoute.USB -> PrinterAvailability(PeripheralStatus.READY, PrinterLink.USB)
            PrinterRoute.USB_PERMISSION -> PrinterAvailability(PeripheralStatus.PERMISSION_REQUIRED, PrinterLink.USB)
            PrinterRoute.BLUETOOTH -> PrinterAvailability(
                if (bluetoothRadioReady(context)) PeripheralStatus.READY else PeripheralStatus.DISCONNECTED,
                PrinterLink.BLUETOOTH,
            )
            PrinterRoute.NONE -> PrinterAvailability(PeripheralStatus.DISCONNECTED, PrinterLink.NONE)
        }
    }

    fun printerStatus(context: Context, mode: RuntimeMode): PeripheralStatus = availability(context, mode).status
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

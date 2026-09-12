package io.github.alexistrejo.pimienta.pos.hardware

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Bridges the ESC/POS encoder to a USB thermal printer transport.
class UsbTicketPrinter private constructor(
    override val profile: PrinterProfile,
    private val transport: UsbPrintTransport,
) : TicketPrinter {
    private val _status = MutableStateFlow(PeripheralStatus.READY)
    override val status: Flow<PeripheralStatus> = _status.asStateFlow()

    override suspend fun print(bytes: ByteArray): PrintResult {
        val result = transport.send(bytes)
        if (result is PrintResult.Failed) _status.value = PeripheralStatus.ERROR
        return result
    }

    fun close() {
        transport.close()
        _status.value = PeripheralStatus.DISCONNECTED
    }

    companion object {
        // Opens the configured USB printer when permission and hardware are available.
        fun open(context: Context): UsbTicketPrinter? {
            val transport = UsbPrintTransport.open(context) ?: return null
            return UsbTicketPrinter(PrinterProfiles.pos5890A, transport)
        }

        fun status(context: Context): PeripheralStatus = UsbPrintTransport.status(context)
    }
}

package io.github.alexistrejo.pimienta.pos.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

// Classic Bluetooth serial UUID used by ESC/POS thermal printers.
internal val ESC_POS_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

// Writes one ESC/POS payload. The caller owns opening and closing the socket.
internal fun writeEscPos(output: OutputStream, bytes: ByteArray) {
    output.write(bytes)
    output.flush()
}

data class BondedPrinter(val name: String, val mac: String)

// Opens a fresh RFCOMM socket for each ticket so a dropped link does not stick for the whole shift.
class BluetoothPrintTransport(
    private val context: Context,
    private val mac: String,
) : PrintTransport {
    private val _status = MutableStateFlow(PeripheralStatus.DISCONNECTED)
    override val status: Flow<PeripheralStatus> = _status.asStateFlow()

    @SuppressLint("MissingPermission")
    override suspend fun send(bytes: ByteArray): PrintResult = withContext(Dispatchers.IO) {
        val radio = try {
            adapter(context)
        } catch (_: SecurityException) {
            return@withContext PrintResult.Failed(PrintFailure.PERMISSION_REQUIRED)
        } ?: return@withContext PrintResult.Failed(PrintFailure.DISCONNECTED)
        val enabled = try {
            radio.isEnabled
        } catch (_: SecurityException) {
            return@withContext PrintResult.Failed(PrintFailure.PERMISSION_REQUIRED)
        }
        if (!enabled) return@withContext PrintResult.Failed(PrintFailure.DISCONNECTED)
        val device = try {
            radio.getRemoteDevice(mac)
        } catch (_: IllegalArgumentException) {
            return@withContext PrintResult.Failed(PrintFailure.DISCONNECTED)
        }
        val socket = try {
            device.createRfcommSocketToServiceRecord(ESC_POS_SPP_UUID)
        } catch (_: SecurityException) {
            return@withContext PrintResult.Failed(PrintFailure.PERMISSION_REQUIRED)
        }
        try {
            _status.value = PeripheralStatus.BUSY
            withTimeout(CONNECT_TIMEOUT_MS) { socket.connect() }
            writeEscPos(socket.outputStream, bytes)
            _status.value = PeripheralStatus.READY
            PrintResult.Printed
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            _status.value = PeripheralStatus.ERROR
            PrintResult.Failed(PrintFailure.TIMEOUT)
        } catch (_: IOException) {
            _status.value = PeripheralStatus.ERROR
            PrintResult.Failed(PrintFailure.TRANSPORT_ERROR)
        } catch (_: SecurityException) {
            _status.value = PeripheralStatus.ERROR
            PrintResult.Failed(PrintFailure.PERMISSION_REQUIRED)
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {
                // The next ticket opens a new socket.
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000L
    }
}

// Sends tickets to the paired thermal printer without holding the socket open.
class BluetoothTicketPrinter(context: Context, mac: String) : TicketPrinter {
    override val profile: PrinterProfile = PrinterProfiles.pos5890A.copy(
        id = "pos-5890a-bluetooth",
        displayName = "POS-5890A · Bluetooth",
    )
    private val transport = BluetoothPrintTransport(context.applicationContext, mac)
    override val status: Flow<PeripheralStatus> = transport.status

    override suspend fun print(bytes: ByteArray): PrintResult = transport.send(bytes)
}

private fun adapter(context: Context): android.bluetooth.BluetoothAdapter? {
    return context.applicationContext.getSystemService(BluetoothManager::class.java)?.adapter
}

// True when the radio can open a socket. Missing permission is treated as unavailable.
@SuppressLint("MissingPermission")
fun bluetoothRadioReady(context: Context): Boolean {
    return try {
        adapter(context)?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }
}

@SuppressLint("MissingPermission")
fun bondedPrinters(context: Context): List<BondedPrinter> {
    return try {
        adapter(context)?.bondedDevices.orEmpty().map { device ->
            BondedPrinter(name = device.name?.takeIf { it.isNotBlank() } ?: device.address, mac = device.address)
        }.sortedBy { it.name }
    } catch (_: SecurityException) {
        emptyList()
    }
}

package io.github.alexistrejo.pimienta.pos.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

// RFCOMM open styles used by cheap ESC/POS printers.
enum class RfcommStrategy { SECURE_SPP, INSECURE_SPP, CHANNEL_1 }

// One connected RFCOMM session. close() must be safe to call more than once.
interface BluetoothRfcommSession {
    fun connect()
    val outputStream: OutputStream
    val inputStream: InputStream
    fun close()
}

// Android radio operations the print transport needs, so JVM tests can fake the socket.
interface BluetoothRfcommLink {
    // Null when the radio can open a socket; otherwise the failure to report without connecting.
    fun checkRadio(): PrintFailure?
    fun cancelDiscovery()
    fun open(strategy: RfcommStrategy): BluetoothRfcommSession
}

// Prefers the last strategy that printed, then the remaining fallbacks.
internal fun rfcommStrategiesStartingWith(preferred: RfcommStrategy?): List<RfcommStrategy> {
    val all = RfcommStrategy.entries
    if (preferred == null) return all
    return listOf(preferred) + all.filter { it != preferred }
}

// Opens RFCOMM sockets on a bonded MAC using the three compatibility strategies.
class AndroidBluetoothRfcommLink(
    context: Context,
    private val mac: String,
) : BluetoothRfcommLink {
    private val app = context.applicationContext

    @SuppressLint("MissingPermission")
    override fun checkRadio(): PrintFailure? {
        val radio = try {
            adapter()
        } catch (_: SecurityException) {
            return PrintFailure.PERMISSION_REQUIRED
        } ?: return PrintFailure.DISCONNECTED
        val enabled = try {
            radio.isEnabled
        } catch (_: SecurityException) {
            return PrintFailure.PERMISSION_REQUIRED
        }
        if (!enabled) return PrintFailure.DISCONNECTED
        return try {
            val device = radio.getRemoteDevice(mac)
            // An unpaired MAC would block a background worker on a pairing dialog. Fail before RFCOMM.
            if (device.bondState != BluetoothDevice.BOND_BONDED) PrintFailure.DISCONNECTED else null
        } catch (_: IllegalArgumentException) {
            PrintFailure.DISCONNECTED
        } catch (_: SecurityException) {
            PrintFailure.PERMISSION_REQUIRED
        }
    }

    @SuppressLint("MissingPermission")
    override fun cancelDiscovery() {
        try {
            adapter()?.cancelDiscovery()
        } catch (_: SecurityException) {
            // Connect may still succeed without cancelling inquiry.
        }
    }

    @SuppressLint("MissingPermission")
    override fun open(strategy: RfcommStrategy): BluetoothRfcommSession {
        val radio = adapter() ?: throw IOException("Bluetooth adapter missing")
        val device = radio.getRemoteDevice(mac)
        val socket = when (strategy) {
            RfcommStrategy.SECURE_SPP -> device.createRfcommSocketToServiceRecord(ESC_POS_SPP_UUID)
            RfcommStrategy.INSECURE_SPP -> device.createInsecureRfcommSocketToServiceRecord(ESC_POS_SPP_UUID)
            RfcommStrategy.CHANNEL_1 -> {
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                method.invoke(device, 1) as BluetoothSocket
            }
        }
        return SocketSession(socket)
    }

    private fun adapter() = app.getSystemService(BluetoothManager::class.java)?.adapter

    private class SocketSession(private val socket: BluetoothSocket) : BluetoothRfcommSession {
        override fun connect() = socket.connect()
        override val outputStream: OutputStream get() = socket.outputStream
        override val inputStream: InputStream get() = socket.inputStream
        override fun close() {
            try {
                socket.close()
            } catch (_: IOException) {
                // Closed safely.
            }
        }
    }
}

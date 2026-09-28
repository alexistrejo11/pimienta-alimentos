package io.github.alexistrejo.pimienta.pos.hardware

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Shares printer attach/permission/ACL events and owns the process Bluetooth ticket printer.
object PosPrinterRegistry {
    private val _statusTick = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val statusTick = _statusTick.asSharedFlow()
    private val lock = Any()
    private var bluetooth: BluetoothTicketPrinter? = null
    private var bluetoothMac: String? = null

    // Bumps live printer chips and Manager Estado without polling-only refresh.
    fun notifyChanged() {
        _statusTick.tryEmit(Unit)
    }

    // Reuses one RFCOMM owner per saved MAC so WorkManager and the UI do not open two sockets.
    fun bluetoothPrinter(context: Context, mac: String): BluetoothTicketPrinter = synchronized(lock) {
        val current = bluetooth
        if (current != null && bluetoothMac.equals(mac, ignoreCase = true)) return current
        current?.close()
        BluetoothTicketPrinter(context.applicationContext, mac).also {
            bluetooth = it
            bluetoothMac = mac
        }
    }

    fun releaseBluetooth() = synchronized(lock) {
        bluetooth?.close()
        bluetooth = null
        bluetoothMac = null
        notifyChanged()
    }

    fun bluetoothStatus(): PeripheralStatus = synchronized(lock) {
        bluetooth?.currentStatus ?: PeripheralStatus.DISCONNECTED
    }

    // Updates the floor chip and kept socket when Classic Bluetooth ACL changes for the saved printer.
    // Linked means the radio is up. Ready is reserved for a socket that has already accepted a write.
    fun onBluetoothAcl(context: Context, deviceMac: String, connected: Boolean) {
        val saved = PrinterPreferences(context).mac() ?: return
        if (!saved.equals(deviceMac, ignoreCase = true)) return
        if (connected) {
            bluetoothPrinter(context, saved).noteLinked()
            notifyChanged()
        } else {
            synchronized(lock) { bluetooth?.noteUnlinked() }
            notifyChanged()
        }
    }

    fun onBluetoothRadioOff() {
        synchronized(lock) { bluetooth?.noteUnlinked() }
        notifyChanged()
    }
}

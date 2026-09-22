package io.github.alexistrejo.pimienta.pos.hardware

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Sends raw ESC/POS bytes to a connected USB printer endpoint.
class UsbPrintTransport(
    context: Context,
    private val device: UsbDevice,
    private val connection: UsbDeviceConnection,
    private val endpoint: UsbEndpoint,
) : PrintTransport {
    private val _status = MutableStateFlow(PeripheralStatus.READY)
    override val status: Flow<PeripheralStatus> = _status.asStateFlow()

    override suspend fun send(bytes: ByteArray): PrintResult {
        if (_status.value != PeripheralStatus.READY) return PrintResult.Failed(PrintFailure.DISCONNECTED)
        var offset = 0
        while (offset < bytes.size) {
            val chunk = bytes.copyOfRange(offset, minOf(offset + endpoint.maxPacketSize, bytes.size))
            val written = connection.bulkTransfer(endpoint, chunk, chunk.size, 5_000)
            if (written <= 0) return PrintResult.Failed(PrintFailure.TRANSPORT_ERROR)
            offset += written
        }
        return PrintResult.Printed
    }

    fun close() {
        connection.close()
        _status.value = PeripheralStatus.DISCONNECTED
    }

    companion object {
        const val ACTION_USB_PERMISSION = "io.github.alexistrejo.pimienta.pos.USB_PERMISSION"
        internal val permissionGate = UsbPermissionGate()

        // Opens the first compatible USB printer discovered on the bus.
        fun open(context: Context): UsbPrintTransport? {
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val device = manager.deviceList.values.firstOrNull(::isPrinterCandidate) ?: return null
            if (!manager.hasPermission(device)) {
                requestPermission(context, manager, device)
                return null
            }
            return openDevice(context, manager, device)
        }

        // Returns true when a USB printer is visible, even if permission is still pending.
        fun status(context: Context): PeripheralStatus {
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val device = manager.deviceList.values.firstOrNull(::isPrinterCandidate) ?: return PeripheralStatus.DISCONNECTED
            return if (manager.hasPermission(device)) PeripheralStatus.READY else PeripheralStatus.PERMISSION_REQUIRED
        }

        // Prompts for USB access when a printer is visible but not yet authorized.
        fun requestPermissionIfNeeded(context: Context, device: UsbDevice? = null) {
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val target = device ?: manager.deviceList.values.firstOrNull(::isPrinterCandidate) ?: return
            if (!isPrinterCandidate(target)) return
            if (!manager.hasPermission(target)) {
                requestPermission(context, manager, target)
            }
        }

        fun isPrinterCandidate(device: UsbDevice): Boolean {
            val interfaces = (0 until device.interfaceCount).map { device.getInterface(it).interfaceClass }
            return permissionGate.isPrinterCandidate(device.deviceClass, interfaces)
        }

        fun markPermissionResolved(device: UsbDevice) {
            permissionGate.markResolved(device.deviceId)
        }

        @Suppress("DEPRECATION")
        fun deviceFrom(intent: Intent): UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

        private fun requestPermission(context: Context, manager: UsbManager, device: UsbDevice): Boolean {
            val isPrinter = isPrinterCandidate(device)
            if (!permissionGate.shouldPrompt(device.deviceId, isPrinter, manager.hasPermission(device))) {
                return false
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                flags,
            )
            manager.requestPermission(device, pending)
            return false
        }

        private fun openDevice(context: Context, manager: UsbManager, device: UsbDevice): UsbPrintTransport? {
            val connection = manager.openDevice(device) ?: return null
            for (index in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(index)
                val endpoint = findBulkOutEndpoint(usbInterface) ?: continue
                if (!connection.claimInterface(usbInterface, true)) continue
                return UsbPrintTransport(context, device, connection, endpoint)
            }
            connection.close()
            return null
        }

        private fun findBulkOutEndpoint(usbInterface: UsbInterface): UsbEndpoint? {
            for (endpointIndex in 0 until usbInterface.endpointCount) {
                val endpoint = usbInterface.getEndpoint(endpointIndex)
                if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK && endpoint.direction == UsbConstants.USB_DIR_OUT) {
                    return endpoint
                }
            }
            return null
        }
    }
}

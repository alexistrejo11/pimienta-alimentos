package io.github.alexistrejo.pimienta.pos.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import java.util.UUID

// Official Bluetooth assigned class bits (avoid Android stub constants in unit tests).
internal const val BT_MAJOR_PERIPHERAL = 0x0500
internal const val BT_MAJOR_IMAGING = 0x0600
internal const val BT_DEVICE_PERIPHERAL_KEYBOARD = 0x0540
internal const val BT_DEVICE_PERIPHERAL_KEYBOARD_POINTING = 0x05C0
internal const val BT_DEVICE_IMAGING_PRINTER = 0x0680

// HID profile UUID; barcode wedges in keyboard mode advertise this, not a printer SPP socket.
internal val HID_UUID: UUID = UUID.fromString("00001124-0000-1000-8000-00805F9B34FB")

enum class BondedBluetoothKind { PRINTER, SCANNER, OTHER }

data class BondedBluetoothDevice(
    val name: String,
    val mac: String,
    val kind: BondedBluetoothKind,
)

// Classifies a bonded radio using Bluetooth class bits, profile UUIDs, then a conservative name hint.
fun classifyBondedBluetooth(
    name: String,
    majorClass: Int,
    deviceClass: Int,
    uuids: Collection<UUID>,
): BondedBluetoothKind {
    val uuidSet = uuids.toSet()
    val hid = HID_UUID in uuidSet ||
        majorClass == BT_MAJOR_PERIPHERAL ||
        deviceClass == BT_DEVICE_PERIPHERAL_KEYBOARD ||
        deviceClass == BT_DEVICE_PERIPHERAL_KEYBOARD_POINTING
    if (hid) return BondedBluetoothKind.SCANNER

    val printerClass = majorClass == BT_MAJOR_IMAGING ||
        deviceClass == BT_DEVICE_IMAGING_PRINTER
    if (printerClass || ESC_POS_SPP_UUID in uuidSet) return BondedBluetoothKind.PRINTER

    val lowered = name.lowercase()
    if (looksLikeScannerName(lowered)) return BondedBluetoothKind.SCANNER
    if (looksLikePrinterName(lowered)) return BondedBluetoothKind.PRINTER
    return BondedBluetoothKind.OTHER
}

private fun looksLikeScannerName(lowered: String): Boolean =
    listOf("scan", "shawty", "lector", "barcode", "honeywell", "zebra").any { it in lowered }

private fun looksLikePrinterName(lowered: String): Boolean =
    listOf("print", "pos-", "5890", "zj-", "thermal", "esc/pos", "escpos").any { it in lowered }

@SuppressLint("MissingPermission")
fun bondedBluetoothDevices(context: Context): List<BondedBluetoothDevice> {
    return try {
        adapterOrNull(context)?.bondedDevices.orEmpty().map { device ->
            val name = device.name?.takeIf { it.isNotBlank() } ?: device.address
            val klass = device.bluetoothClass
            val uuids = device.uuids.orEmpty().map { it.uuid }
            BondedBluetoothDevice(
                name = name,
                mac = device.address,
                kind = classifyBondedBluetooth(
                    name = name,
                    majorClass = klass?.majorDeviceClass ?: 0,
                    deviceClass = klass?.deviceClass ?: 0,
                    uuids = uuids,
                ),
            )
        }.sortedBy { it.name }
    } catch (_: SecurityException) {
        emptyList()
    }
}

fun bondedPrinters(context: Context): List<BondedPrinter> =
    bondedBluetoothDevices(context)
        .filter { it.kind == BondedBluetoothKind.PRINTER }
        .map { BondedPrinter(name = it.name, mac = it.mac) }

private fun adapterOrNull(context: Context) =
    context.applicationContext.getSystemService(BluetoothManager::class.java)?.adapter

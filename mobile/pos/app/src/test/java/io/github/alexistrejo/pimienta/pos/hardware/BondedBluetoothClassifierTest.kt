package io.github.alexistrejo.pimienta.pos.hardware

import org.junit.Assert.assertEquals
import org.junit.Test

// HID readers must never be offered as a print destination.
class BondedBluetoothClassifierTest {
    @Test
    fun hidUuidIsAScannerEvenWithSpp() {
        val kind = classifyBondedBluetooth(
            name = "Shawty S0024",
            majorClass = 0,
            deviceClass = 0,
            uuids = listOf(HID_UUID, ESC_POS_SPP_UUID),
        )
        assertEquals(BondedBluetoothKind.SCANNER, kind)
    }

    @Test
    fun peripheralKeyboardClassIsAScanner() {
        val kind = classifyBondedBluetooth(
            name = "Barcode Scanner",
            majorClass = BT_MAJOR_PERIPHERAL,
            deviceClass = BT_DEVICE_PERIPHERAL_KEYBOARD,
            uuids = emptyList(),
        )
        assertEquals(BondedBluetoothKind.SCANNER, kind)
    }

    @Test
    fun imagingPrinterClassIsAPrinter() {
        val kind = classifyBondedBluetooth(
            name = "POS-5890A",
            majorClass = BT_MAJOR_IMAGING,
            deviceClass = BT_DEVICE_IMAGING_PRINTER,
            uuids = emptyList(),
        )
        assertEquals(BondedBluetoothKind.PRINTER, kind)
    }

    @Test
    fun sppWithoutHidIsAPrinter() {
        val kind = classifyBondedBluetooth(
            name = "ZJ-5890A",
            majorClass = 0x1F00,
            deviceClass = 0,
            uuids = listOf(ESC_POS_SPP_UUID),
        )
        assertEquals(BondedBluetoothKind.PRINTER, kind)
    }

    @Test
    fun shawtyNameWithoutClassIsAScanner() {
        val kind = classifyBondedBluetooth(
            name = "Shawty-S0024",
            majorClass = 0,
            deviceClass = 0,
            uuids = emptyList(),
        )
        assertEquals(BondedBluetoothKind.SCANNER, kind)
    }

    @Test
    fun unknownPhoneStaysOther() {
        val kind = classifyBondedBluetooth(
            name = "Pixel 8",
            majorClass = 0x0200,
            deviceClass = 0,
            uuids = emptyList(),
        )
        assertEquals(BondedBluetoothKind.OTHER, kind)
    }

    @Test
    fun pos5890NameWithoutUuidsIsAPrinter() {
        val kind = classifyBondedBluetooth(
            name = "POS-5890A",
            majorClass = 0,
            deviceClass = 0,
            uuids = emptyList(),
        )
        assertEquals(BondedBluetoothKind.PRINTER, kind)
    }

    @Test
    fun zj5890NameWithoutUuidsIsAPrinter() {
        val kind = classifyBondedBluetooth(
            name = "ZJ-5890A",
            majorClass = 0,
            deviceClass = 0,
            uuids = emptyList(),
        )
        assertEquals(BondedBluetoothKind.PRINTER, kind)
    }
}

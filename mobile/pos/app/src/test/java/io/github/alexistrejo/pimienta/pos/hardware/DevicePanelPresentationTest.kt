package io.github.alexistrejo.pimienta.pos.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Checks the cashier-facing printer copy for each cable / Bluetooth combination.
class DevicePanelPresentationTest {
    private val mac = "AA:BB:CC:DD:EE:FF"

    private fun input(
        usb: PeripheralStatus = PeripheralStatus.DISCONNECTED,
        savedMac: String? = mac,
        radio: Boolean = true,
        bt: PeripheralStatus = PeripheralStatus.DISCONNECTED,
        training: Boolean = false,
    ) = PrinterPanelInput(usb, savedMac, "POS-5890A", radio, bt, training)

    @Test
    fun cableWinsAndNamesTheBluetoothBackup() {
        val line = printerStatusLine(input(usb = PeripheralStatus.READY))
        assertEquals("Lista · por cable USB", line.title)
        assertTrue(line.detail!!.contains("POS-5890A"))
        assertEquals(DeviceTone.OK, line.tone)
        assertNull(line.fix)
    }

    @Test
    fun pendingUsbPermissionKeepsPrintingByBluetoothAndOffersTheFix() {
        val line = printerStatusLine(input(usb = PeripheralStatus.PERMISSION_REQUIRED))
        assertTrue(line.title.startsWith("Imprimiendo por Bluetooth"))
        assertEquals(DeviceTone.WARNING, line.tone)
        assertEquals(DeviceFix.GRANT_USB, line.fix)
    }

    @Test
    fun pendingUsbPermissionWithoutBluetoothBlocks() {
        val line = printerStatusLine(input(usb = PeripheralStatus.PERMISSION_REQUIRED, savedMac = null))
        assertEquals(DeviceTone.BLOCKED, line.tone)
        assertEquals(DeviceFix.GRANT_USB, line.fix)
    }

    @Test
    fun radioOffPointsToBluetoothSettings() {
        val line = printerStatusLine(input(radio = false))
        assertEquals("Bluetooth apagado", line.title)
        assertEquals(DeviceFix.OPEN_BLUETOOTH_SETTINGS, line.fix)
    }

    @Test
    fun idleBluetoothIsAWarningNotAnError() {
        assertEquals(DeviceTone.WARNING, printerStatusLine(input()).tone)
        assertEquals(DeviceTone.OK, printerStatusLine(input(bt = PeripheralStatus.READY)).tone)
        assertEquals(DeviceTone.BLOCKED, printerStatusLine(input(bt = PeripheralStatus.ERROR)).tone)
    }

    @Test
    fun noPrinterIsBlockedOutsideTraining() {
        assertEquals(DeviceTone.BLOCKED, printerStatusLine(input(savedMac = null)).tone)
        assertEquals(DeviceTone.WARNING, printerStatusLine(input(savedMac = null, training = true)).tone)
    }

    @Test
    fun failureMessagesNeverLeakEnumNames() {
        PrintFailure.entries.forEach { reason ->
            assertFalse(printFailureMessage(reason).contains(reason.name))
        }
    }
}

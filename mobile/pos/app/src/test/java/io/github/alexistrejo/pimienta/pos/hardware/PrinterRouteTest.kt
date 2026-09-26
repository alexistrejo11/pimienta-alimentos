package io.github.alexistrejo.pimienta.pos.hardware

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

// Chooses USB, a pending USB permission, Bluetooth, or no printer without opening a socket.
class PrinterRouteTest {
    @Test
    fun usbReadyWinsOverASavedMac() {
        assertEquals(PrinterRoute.USB, choosePrinterRoute(PeripheralStatus.READY, "AA:BB:CC:DD:EE:FF"))
        assertEquals(PrinterLink.USB, printerLinkFor(PrinterRoute.USB))
    }

    @Test
    fun usbPermissionDoesNotFallThroughToBluetooth() {
        assertEquals(
            PrinterRoute.USB_PERMISSION,
            choosePrinterRoute(PeripheralStatus.PERMISSION_REQUIRED, "AA:BB:CC:DD:EE:FF"),
        )
    }

    @Test
    fun savedMacIsUsedWhenUsbIsAbsent() {
        assertEquals(PrinterRoute.BLUETOOTH, choosePrinterRoute(PeripheralStatus.DISCONNECTED, "AA:BB:CC:DD:EE:FF"))
        assertEquals(PrinterLink.BLUETOOTH, printerLinkFor(PrinterRoute.BLUETOOTH))
    }

    @Test
    fun missingUsbAndMacLeavesTheQueueWithoutAPrinter() {
        assertEquals(PrinterRoute.NONE, choosePrinterRoute(PeripheralStatus.DISCONNECTED, null))
        assertEquals(PrinterRoute.NONE, choosePrinterRoute(PeripheralStatus.ERROR, " "))
        assertEquals(PrinterLink.NONE, printerLinkFor(PrinterRoute.NONE))
    }

    @Test
    fun writeEscPosFlushesThePayload() {
        val output = ByteArrayOutputStream()
        val payload = byteArrayOf(0x1B, 0x40, 0x70)
        writeEscPos(output, payload)
        assertArrayEquals(payload, output.toByteArray())
    }
}

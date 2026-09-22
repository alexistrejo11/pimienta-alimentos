package io.github.alexistrejo.pimienta.pos.hardware

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Manual tablet check: cart survives hub plug; sangria/corte print if the printer is already allowed; one USB dialog.
class UsbPermissionGateTest {
    private val hidClass = 3
    private val printerClass = UsbPermissionGate.USB_CLASS_PRINTER

    @Test
    fun printerClassOrPrinterInterfaceIsACandidate() {
        val gate = UsbPermissionGate()
        assertTrue(gate.isPrinterCandidate(printerClass, emptyList()))
        assertTrue(gate.isPrinterCandidate(0, listOf(hidClass, printerClass)))
        assertFalse(gate.isPrinterCandidate(hidClass, listOf(hidClass)))
        assertFalse(gate.isPrinterCandidate(9, emptyList()))
    }

    @Test
    fun threeAttachesOfTheSamePrinterPromptOnce() {
        val gate = UsbPermissionGate()
        val deviceId = 42
        assertTrue(gate.shouldPrompt(deviceId, isPrinter = true, hasPermission = false))
        assertFalse(gate.shouldPrompt(deviceId, isPrinter = true, hasPermission = false))
        assertFalse(gate.shouldPrompt(deviceId, isPrinter = true, hasPermission = false))
    }

    @Test
    fun hidAttachDoesNotPrompt() {
        val gate = UsbPermissionGate()
        assertFalse(gate.shouldPrompt(7, isPrinter = false, hasPermission = false))
    }

    @Test
    fun grantedPrinterDoesNotPrompt() {
        val gate = UsbPermissionGate()
        assertFalse(gate.shouldPrompt(7, isPrinter = true, hasPermission = true))
    }

    @Test
    fun detachAllowsALaterPrompt() {
        val gate = UsbPermissionGate()
        assertTrue(gate.shouldPrompt(7, isPrinter = true, hasPermission = false))
        gate.markResolved(7)
        assertTrue(gate.shouldPrompt(7, isPrinter = true, hasPermission = false))
    }
}

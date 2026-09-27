package io.github.alexistrejo.pimienta.pos.hardware

import android.view.KeyEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// Converts USB/Bluetooth HID keyboard-wedge scans into normalized barcode events.
class HidKeyboardBarcodeScanner : BarcodeScanner {
    private val _status = MutableStateFlow(PeripheralStatus.DISCONNECTED)
    private val _events = MutableSharedFlow<BarcodeRead>(extraBufferCapacity = 32)
    private val burst = HidScanBurstHelper()
    @Volatile private var capturing = false

    override val status: Flow<PeripheralStatus> = _status.asStateFlow()
    override val events: Flow<BarcodeRead> = _events.asSharedFlow()

    // Captures wedge keys at the activity; Enter is always swallowed so it cannot click Compose buttons.
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!capturing) return false
        if (event.repeatCount > 0) return false
        if (HidKeySource.shouldIgnoreHumanKeyboard(
                deviceId = event.deviceId,
                flags = event.flags,
                isVirtualDevice = event.device?.isVirtual ?: true,
                source = event.source,
            )
        ) {
            return false
        }
        val enter = HidKeySource.isEnterKeyCode(event.keyCode)
        if (event.action != KeyEvent.ACTION_DOWN) {
            return enter
        }
        if (enter) {
            val code = burst.onEnter()
            if (code != null) {
                PosScannerRegistry.noteRead(code)
                _events.tryEmit(BarcodeRead(code, ScannerSource.USB_HID))
            }
            return true
        }
        val char = HidKeySource.barcodeChar(event.keyCode, event.unicodeChar) ?: return false
        return burst.onCharacter(char)
    }

    override suspend fun start() {
        burst.reset()
        capturing = true
        _status.value = PeripheralStatus.READY
    }

    override suspend fun stop() {
        capturing = false
        burst.reset()
        _status.value = PeripheralStatus.DISCONNECTED
    }
}

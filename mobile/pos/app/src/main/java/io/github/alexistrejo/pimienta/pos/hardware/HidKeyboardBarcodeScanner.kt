package io.github.alexistrejo.pimienta.pos.hardware

import android.view.KeyEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// Converts USB-HID keyboard-wedge scans into normalized barcode events.
class HidKeyboardBarcodeScanner : BarcodeScanner {
    private val _status = MutableStateFlow(PeripheralStatus.DISCONNECTED)
    private val _events = MutableSharedFlow<BarcodeRead>(extraBufferCapacity = 32)
    private val burst = HidScanBurstHelper()
    @Volatile private var capturing = false

    override val status: Flow<PeripheralStatus> = _status.asStateFlow()
    override val events: Flow<BarcodeRead> = _events.asSharedFlow()

    // Captures USB/Bluetooth scanner key events at the activity dispatch layer.
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!capturing) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
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
        return when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                val code = burst.onEnter()
                if (code != null) {
                    _events.tryEmit(BarcodeRead(code, ScannerSource.USB_HID))
                    true
                } else {
                    false
                }
            }
            else -> {
                val char = event.unicodeChar.toChar()
                if (char.isLetterOrDigit() || char in "-._/") {
                    burst.onCharacter(char)
                } else {
                    false
                }
            }
        }
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

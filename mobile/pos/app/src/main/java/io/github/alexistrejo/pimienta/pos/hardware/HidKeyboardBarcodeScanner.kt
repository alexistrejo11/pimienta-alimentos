package io.github.alexistrejo.pimienta.pos.hardware

import android.view.KeyEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// Converts USB-HID keyboard-wedge scans into normalized barcode events.
class HidKeyboardBarcodeScanner : BarcodeScanner {
    private val _status = MutableStateFlow(PeripheralStatus.READY)
    private val _events = MutableSharedFlow<BarcodeRead>(extraBufferCapacity = 32)
    private val burst = HidScanBurstHelper()

    override val status: Flow<PeripheralStatus> = _status.asStateFlow()
    override val events: Flow<BarcodeRead> = _events.asSharedFlow()

    // Consumes only fast wedge bursts so slow typing can reach Compose search fields.
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val now = System.currentTimeMillis()
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
                    burst.onCharacter(char, now)
                } else {
                    false
                }
            }
        }
    }

    override suspend fun start() {
        burst.reset()
        _status.value = PeripheralStatus.READY
    }

    override suspend fun stop() {
        burst.reset()
        _status.value = PeripheralStatus.DISCONNECTED
    }
}

package io.github.alexistrejo.pimienta.pos.hardware

// Shares the active scanner instances between sale and manager diagnostics.
object PosScannerRegistry {
    var primary: BarcodeScanner? = null
    var fake: FakeBarcodeScanner? = null
    var hid: HidKeyboardBarcodeScanner? = null
}

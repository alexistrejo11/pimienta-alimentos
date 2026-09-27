package io.github.alexistrejo.pimienta.pos.hardware

// Shares scanner instances and the last successful read for device presence.
object PosScannerRegistry {
    var primary: BarcodeScanner? = null
    var fake: FakeBarcodeScanner? = null
    var hid: HidKeyboardBarcodeScanner? = null

    @Volatile
    var lastReadAtMillis: Long = 0
        private set

    @Volatile
    var lastReadValue: String? = null
        private set

    fun noteRead(rawValue: String) {
        lastReadValue = rawValue
        lastReadAtMillis = System.currentTimeMillis()
    }

    fun recentRead(nowMillis: Long = System.currentTimeMillis(), windowMs: Long = 120_000): Boolean {
        val at = lastReadAtMillis
        return at > 0 && nowMillis - at < windowMs
    }
}

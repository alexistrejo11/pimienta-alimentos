package io.github.alexistrejo.pimienta.pos.hardware

// Accumulates HID wedge characters and only treats a fast burst as a completed barcode.
internal class HidScanBurstHelper(
    private val minLength: Int = 4,
    private val maxIntervalMs: Long = 80L,
) {
    private val buffer = StringBuilder()
    private var lastEventAtMs: Long = 0L

    // Consumes hardware wedge characters; on-screen keyboards are filtered before this is called.
    fun onCharacter(char: Char, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (buffer.isNotEmpty() && nowMs - lastEventAtMs > maxIntervalMs) {
            buffer.clear()
        }
        buffer.append(char)
        lastEventAtMs = nowMs
        return true
    }

    // Returns a completed barcode when Enter closes a long-enough burst; otherwise clears state.
    fun onEnter(): String? {
        val code = buffer.toString().trim()
        buffer.clear()
        lastEventAtMs = 0L
        return code.takeIf { it.length >= minLength }
    }

    fun reset() {
        buffer.clear()
        lastEventAtMs = 0L
    }
}

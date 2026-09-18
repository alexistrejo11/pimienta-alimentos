package io.github.alexistrejo.pimienta.pos.hardware

// Pure-Kotlin wedge helper that accumulates HID scanner characters until Enter.
internal class HidScanBurstHelper(
    private val minLength: Int = 1,
) {
    private val buffer = StringBuilder()

    // Consumes characters directly into the scanner buffer.
    fun onCharacter(char: Char): Boolean {
        buffer.append(char)
        return true
    }

    // Returns a completed barcode when Enter closes a scan; otherwise clears state.
    fun onEnter(): String? {
        val code = buffer.toString().trim()
        buffer.clear()
        return code.takeIf { it.length >= minLength }
    }

    fun reset() {
        buffer.clear()
    }
}

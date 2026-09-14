package io.github.alexistrejo.pimienta.pos.hardware

// Pure-Kotlin burst detection so HID scans do not steal slow typing in search fields.
internal class HidScanBurstHelper(
    private val maxGapMs: Long = 80L,
    private val minLength: Int = 4,
) {
    private val buffer = StringBuilder()
    private var lastKeyAt = 0L
    private var inBurst = false

    // Returns true when the character should be consumed at the activity dispatch layer.
    fun onCharacter(char: Char, nowMs: Long): Boolean {
        if (buffer.isEmpty()) {
            buffer.append(char)
            lastKeyAt = nowMs
            inBurst = false
            return false
        }
        val gap = nowMs - lastKeyAt
        return if (gap <= maxGapMs) {
            inBurst = true
            buffer.append(char)
            lastKeyAt = nowMs
            true
        } else {
            buffer.clear()
            buffer.append(char)
            lastKeyAt = nowMs
            inBurst = false
            false
        }
    }

    // Returns a completed barcode when Enter closes a fast burst; otherwise clears state.
    fun onEnter(): String? {
        val code = buffer.toString().trim()
        val accepted = inBurst && code.length >= minLength
        buffer.clear()
        inBurst = false
        return code.takeIf { accepted && it.isNotEmpty() }
    }

    fun reset() {
        buffer.clear()
        inBurst = false
    }
}

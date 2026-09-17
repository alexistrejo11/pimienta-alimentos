package io.github.alexistrejo.pimienta.pos.data.sync

/**
 * Picks the next local deviceSequence after enroll/bootstrap.
 * Same physical tablet must continue past any sequence the server already accepted.
 */
object DeviceSequenceClock {
    fun next(localNext: Long?, serverLast: Long?): Long {
        val fromLocal = localNext ?: 1L
        val fromServer = (serverLast ?: 0L) + 1L
        return maxOf(fromLocal, fromServer)
    }
}

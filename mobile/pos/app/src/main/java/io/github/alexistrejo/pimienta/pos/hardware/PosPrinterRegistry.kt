package io.github.alexistrejo.pimienta.pos.hardware

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Shares USB printer attach/permission events with Compose status surfaces.
object PosPrinterRegistry {
    private val _statusTick = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val statusTick = _statusTick.asSharedFlow()

    // Bumps live printer chips and Manager Estado without polling-only refresh.
    fun notifyChanged() {
        _statusTick.tryEmit(Unit)
    }
}

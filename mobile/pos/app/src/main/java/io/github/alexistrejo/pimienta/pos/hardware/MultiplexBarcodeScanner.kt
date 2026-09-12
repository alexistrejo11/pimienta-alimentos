package io.github.alexistrejo.pimienta.pos.hardware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// Merges multiple scanner sources into one event stream for the sale screen.
class MultiplexBarcodeScanner(
    private val children: List<BarcodeScanner>,
) : BarcodeScanner {
    private val _events = MutableSharedFlow<BarcodeRead>(extraBufferCapacity = 64)
    private var attached = false

    override val status: Flow<PeripheralStatus> = combine(children.map { it.status }) { statuses ->
        when {
            statuses.any { it == PeripheralStatus.READY } -> PeripheralStatus.READY
            statuses.any { it == PeripheralStatus.BUSY } -> PeripheralStatus.BUSY
            statuses.any { it == PeripheralStatus.PERMISSION_REQUIRED } -> PeripheralStatus.PERMISSION_REQUIRED
            statuses.any { it == PeripheralStatus.ERROR } -> PeripheralStatus.ERROR
            statuses.any { it == PeripheralStatus.DISCOVERED } -> PeripheralStatus.DISCOVERED
            else -> PeripheralStatus.DISCONNECTED
        }
    }

    override val events: Flow<BarcodeRead> = _events.asSharedFlow()

    // Starts forwarding child events into the shared stream once per app session.
    fun attach(scope: CoroutineScope) {
        if (attached) return
        attached = true
        children.forEach { child ->
            scope.launch {
                child.events.collect { read ->
                    _events.emit(read)
                }
            }
        }
    }

    override suspend fun start() {
        children.forEach { it.start() }
    }

    override suspend fun stop() {
        children.forEach { it.stop() }
    }
}

package io.github.alexistrejo.pimienta.pos.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// Classic Bluetooth serial UUID used by ESC/POS thermal printers.
internal val ESC_POS_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

// Real-time paper sensor query. Both high bits in the reply mean the roll is out.
private val PAPER_PROBE = byteArrayOf(0x10, 0x04, 0x04)
private const val PAPER_OUT_BITS = 0x60

// Writes one ESC/POS payload. The caller owns opening and closing the socket.
internal fun writeEscPos(output: OutputStream, bytes: ByteArray) {
    output.write(bytes)
    output.flush()
}

// Pauses and chunk sizes for cheap ESC/POS radios. Tests pass a zeroed copy.
data class BluetoothLinkTimings(
    val connectTimeoutMs: Long = 8_000L,
    val writeTimeoutMs: Long = 4_000L,
    val settleAfterConnectMs: Long = 250L,
    val strategyGapMs: Long = 300L,
    val failureCooldownMs: Long = 2_000L,
    val paperProbeMs: Long = 150L,
    val chunkSize: Int = 256,
    val chunkGapMs: Long = 20L,
) {
    companion object {
        val Immediate = BluetoothLinkTimings(
            settleAfterConnectMs = 0L,
            strategyGapMs = 0L,
            failureCooldownMs = 0L,
            paperProbeMs = 0L,
            chunkGapMs = 0L,
        )
    }
}

data class BondedPrinter(val name: String, val mac: String)

private class RfcommTimeout : IOException("RFCOMM timed out")

private class PaperEmpty : IOException("paper empty")

// Keeps one RFCOMM session for the process and reconnects once when a write drops.
class BluetoothPrintTransport(
    private val link: BluetoothRfcommLink,
    private val preferredStrategy: () -> RfcommStrategy?,
    private val rememberStrategy: (RfcommStrategy) -> Unit,
    private val timings: BluetoothLinkTimings = BluetoothLinkTimings(),
    private val nowMs: () -> Long = System::currentTimeMillis,
) : PrintTransport {
    private val mutex = Mutex()
    private val sessionLock = Any()
    private val generation = AtomicInteger(0)
    private val _status = MutableStateFlow(PeripheralStatus.DISCONNECTED)
    private var session: BluetoothRfcommSession? = null
    private var connecting: BluetoothRfcommSession? = null
    private var cooldownUntilMs = 0L

    override val status: Flow<PeripheralStatus> = _status.asStateFlow()
    val currentStatus: PeripheralStatus get() = _status.value

    override suspend fun send(bytes: ByteArray): PrintResult = mutex.withLock {
        withContext(Dispatchers.IO) { sendLocked(bytes) }
    }

    // ACL is up. Ready stays reserved for a socket that has already accepted a write.
    fun noteLinked() {
        val status = _status.value
        if (status == PeripheralStatus.BUSY || status == PeripheralStatus.READY || status == PeripheralStatus.ERROR) return
        _status.value = PeripheralStatus.DISCOVERED
    }

    // Drops the kept socket when the printer or radio goes away.
    fun noteUnlinked() {
        release()
    }

    fun release() {
        val current = synchronized(sessionLock) {
            generation.incrementAndGet()
            val current = session ?: connecting
            session = null
            connecting = null
            _status.value = PeripheralStatus.DISCONNECTED
            current
        }
        current?.close()
    }

    private suspend fun sendLocked(bytes: ByteArray): PrintResult {
        val gen = generation.get()
        val radioFailure = link.checkRadio()
        if (radioFailure != null) {
            _status.value = if (radioFailure == PrintFailure.PERMISSION_REQUIRED) {
                PeripheralStatus.ERROR
            } else {
                PeripheralStatus.DISCONNECTED
            }
            return PrintResult.Failed(radioFailure)
        }
        if (aborted(gen)) return abortedResult()

        val openSession = synchronized(sessionLock) { session }
        if (openSession != null) {
            val busy = synchronized(sessionLock) {
                if (aborted(gen) || session !== openSession) false
                else {
                    _status.value = PeripheralStatus.BUSY
                    true
                }
            }
            if (!busy) return abortedResult()
            try {
                deliver(openSession, bytes)
                if (!markReady(gen)) return abortedResult()
                return PrintResult.Printed
            } catch (_: PaperEmpty) {
                markStatus(gen, openSession, PeripheralStatus.ERROR)
                return PrintResult.Failed(PrintFailure.PAPER_EMPTY)
            } catch (_: IOException) {
                // RfcommTimeout is an IOException. Drop the dead socket and open a new one below.
                dropSession(openSession)
            }
            if (aborted(gen)) return abortedResult()
        }

        if (timings.failureCooldownMs > 0L && nowMs() < cooldownUntilMs) {
            _status.value = PeripheralStatus.ERROR
            return PrintResult.Failed(PrintFailure.TIMEOUT)
        }
        return connectAndWrite(bytes, gen)
    }

    private suspend fun connectAndWrite(bytes: ByteArray, gen: Int): PrintResult {
        var lastFailure: PrintFailure = PrintFailure.TRANSPORT_ERROR
        link.cancelDiscovery()
        val strategies = rfcommStrategiesStartingWith(preferredStrategy())

        for ((index, strategy) in strategies.withIndex()) {
            if (aborted(gen)) return abortedResult()
            if (index > 0 && timings.strategyGapMs > 0L) delay(timings.strategyGapMs)
            if (aborted(gen)) return abortedResult()

            val opened = try {
                link.open(strategy)
            } catch (_: SecurityException) {
                _status.value = PeripheralStatus.ERROR
                return PrintResult.Failed(PrintFailure.PERMISSION_REQUIRED)
            } catch (_: Exception) {
                continue
            }

            val armed = synchronized(sessionLock) {
                if (aborted(gen)) false
                else {
                    connecting = opened
                    _status.value = PeripheralStatus.BUSY
                    true
                }
            }
            if (!armed) {
                opened.close()
                return abortedResult()
            }

            try {
                runWatchdog(opened, timings.connectTimeoutMs) { opened.connect() }
                if (aborted(gen)) {
                    clearConnecting(opened)
                    opened.close()
                    return abortedResult()
                }
                if (timings.settleAfterConnectMs > 0L) delay(timings.settleAfterConnectMs)
                if (aborted(gen)) {
                    clearConnecting(opened)
                    opened.close()
                    return abortedResult()
                }
                try {
                    deliver(opened, bytes)
                } catch (_: PaperEmpty) {
                    rememberStrategy(strategy)
                    if (!publish(gen, opened, PeripheralStatus.ERROR)) return abortedResult()
                    return PrintResult.Failed(PrintFailure.PAPER_EMPTY)
                }
                rememberStrategy(strategy)
                if (!publish(gen, opened, PeripheralStatus.READY)) return abortedResult()
                return PrintResult.Printed
            } catch (_: RfcommTimeout) {
                clearConnecting(opened)
                opened.close()
                if (aborted(gen)) return abortedResult()
                lastFailure = PrintFailure.TIMEOUT
            } catch (_: IOException) {
                clearConnecting(opened)
                opened.close()
                if (aborted(gen)) return abortedResult()
                lastFailure = PrintFailure.TRANSPORT_ERROR
            } catch (_: SecurityException) {
                clearConnecting(opened)
                opened.close()
                _status.value = PeripheralStatus.ERROR
                return PrintResult.Failed(PrintFailure.PERMISSION_REQUIRED)
            }
        }

        if (timings.failureCooldownMs > 0L) {
            cooldownUntilMs = nowMs() + timings.failureCooldownMs
        }
        _status.value = PeripheralStatus.ERROR
        return PrintResult.Failed(lastFailure)
    }

    // Settle is handled by the caller. Here we drop leftover status bytes, ask about paper, then write in chunks.
    private suspend fun deliver(session: BluetoothRfcommSession, bytes: ByteArray) {
        discardIncoming(session)
        if (paperEmpty(session)) throw PaperEmpty()
        val chunkSize = timings.chunkSize.coerceAtLeast(1)
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            runWatchdog(session, timings.writeTimeoutMs) {
                session.outputStream.write(bytes, offset, length)
                session.outputStream.flush()
            }
            offset += length
            if (offset < bytes.size && timings.chunkGapMs > 0L) delay(timings.chunkGapMs)
        }
    }

    // Closes the socket when connect or a chunk blocks, so the IO thread can move on.
    private suspend fun runWatchdog(session: BluetoothRfcommSession, timeoutMs: Long, block: () -> Unit) {
        if (timeoutMs <= 0L) {
            block()
            return
        }
        coroutineScope {
            val timedOut = AtomicBoolean(false)
            val watchdog = launch {
                delay(timeoutMs)
                if (timedOut.compareAndSet(false, true)) session.close()
            }
            try {
                block()
            } catch (error: IOException) {
                if (timedOut.get()) throw RfcommTimeout()
                throw error
            } finally {
                watchdog.cancel()
            }
        }
    }

    private suspend fun paperEmpty(session: BluetoothRfcommSession): Boolean {
        if (timings.paperProbeMs <= 0L) return false
        runWatchdog(session, timings.writeTimeoutMs) {
            session.outputStream.write(PAPER_PROBE)
            session.outputStream.flush()
        }
        delay(timings.paperProbeMs)
        val input = session.inputStream
        if (input.available() <= 0) return false
        val status = input.read()
        return status >= 0 && (status and PAPER_OUT_BITS) == PAPER_OUT_BITS
    }

    private fun discardIncoming(session: BluetoothRfcommSession) {
        val input = session.inputStream
        repeat(32) {
            if (input.available() <= 0) return
            if (input.read() < 0) return
        }
    }

    private fun clearConnecting(opened: BluetoothRfcommSession) {
        synchronized(sessionLock) {
            if (connecting === opened) connecting = null
        }
    }

    private fun dropSession(current: BluetoothRfcommSession) {
        val shouldClose = synchronized(sessionLock) {
            if (session === current) {
                session = null
                true
            } else {
                false
            }
        }
        if (shouldClose) current.close()
    }

    private fun publish(gen: Int, opened: BluetoothRfcommSession, status: PeripheralStatus): Boolean {
        val published = synchronized(sessionLock) {
            if (connecting === opened) connecting = null
            if (aborted(gen)) false
            else {
                session = opened
                _status.value = status
                true
            }
        }
        if (!published) opened.close()
        return published
    }

    private fun markReady(gen: Int): Boolean = synchronized(sessionLock) {
        if (aborted(gen) || session == null) false
        else {
            _status.value = PeripheralStatus.READY
            true
        }
    }

    private fun markStatus(gen: Int, current: BluetoothRfcommSession, status: PeripheralStatus) {
        synchronized(sessionLock) {
            if (!aborted(gen) && session === current) _status.value = status
        }
    }

    private fun aborted(gen: Int): Boolean = generation.get() != gen

    private fun abortedResult(): PrintResult = PrintResult.Failed(PrintFailure.DISCONNECTED)
}

// Sends tickets through the process-wide Bluetooth transport for one saved MAC.
class BluetoothTicketPrinter internal constructor(
    private val transport: BluetoothPrintTransport,
) : TicketPrinter {
    constructor(context: Context, mac: String) : this(
        BluetoothPrintTransport(
            AndroidBluetoothRfcommLink(context.applicationContext, mac),
            preferredStrategy = { PrinterPreferences(context).rfcommStrategy() },
            rememberStrategy = { PrinterPreferences(context).saveRfcommStrategy(it) },
        ),
    )

    override val profile: PrinterProfile = PrinterProfiles.pos5890A.copy(
        id = "pos-5890a-bluetooth",
        displayName = "POS-5890A · Bluetooth",
    )
    override val status: Flow<PeripheralStatus> = transport.status
    val currentStatus: PeripheralStatus get() = transport.currentStatus

    override suspend fun print(bytes: ByteArray): PrintResult = transport.send(bytes)

    fun close() = transport.release()
    fun noteLinked() = transport.noteLinked()
    fun noteUnlinked() = transport.noteUnlinked()
}

private fun adapter(context: Context): android.bluetooth.BluetoothAdapter? {
    return context.applicationContext.getSystemService(BluetoothManager::class.java)?.adapter
}

// True when the radio can open a socket. Missing permission is treated as unavailable.
@SuppressLint("MissingPermission")
fun bluetoothRadioReady(context: Context): Boolean {
    return try {
        adapter(context)?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }
}

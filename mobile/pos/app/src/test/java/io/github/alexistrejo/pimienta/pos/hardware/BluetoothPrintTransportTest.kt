package io.github.alexistrejo.pimienta.pos.hardware

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Verifies RFCOMM fallback, kept sockets, and serialized sends without Android Bluetooth APIs.
class BluetoothPrintTransportTest {
    @Test
    fun rememberedStrategyIsTriedFirstOnTheNextConnect() = runBlocking {
        val link = FakeRfcommLink(failConnect = setOf(RfcommStrategy.SECURE_SPP))
        var preferred: RfcommStrategy? = null
        val transport = transport(link, { preferred }, { preferred = it })

        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(0x1B)))
        assertEquals(listOf(RfcommStrategy.SECURE_SPP, RfcommStrategy.INSECURE_SPP), link.opened)
        assertEquals(RfcommStrategy.INSECURE_SPP, preferred)
        assertEquals(PeripheralStatus.READY, transport.currentStatus)
        assertTrue(link.cancelDiscoveryCount >= 1)

        transport.noteUnlinked()
        link.opened.clear()
        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(0x1B)))
        assertEquals(RfcommStrategy.INSECURE_SPP, link.opened.first())
    }

    @Test
    fun writeFailureReconnectsOnce() = runBlocking {
        val link = FakeRfcommLink()
        val transport = transport(link)

        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(1)))
        val opensAfterFirst = link.opened.size
        link.failNextWrites = 1
        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(2)))
        assertTrue(link.opened.size > opensAfterFirst)
        assertEquals(PeripheralStatus.READY, transport.currentStatus)
    }

    @Test
    fun secondSendReusesTheOpenSocket() = runBlocking {
        val link = FakeRfcommLink()
        val transport = transport(link)

        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(1)))
        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(2)))
        assertEquals(1, link.opened.size)
    }

    @Test
    fun payloadsAreWrittenInChunks() = runBlocking {
        val link = FakeRfcommLink()
        val transport = transport(link, timings = BluetoothLinkTimings.Immediate.copy(chunkSize = 4))

        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(1, 2, 3, 4, 5, 6)))
        assertEquals(2, link.writeCount)
        assertEquals(listOf(1, 2, 3, 4, 5, 6).map { it.toByte() }, link.written.toByteArray().toList())
    }

    @Test
    fun concurrentSendsDoNotOverlap() = runBlocking {
        val link = FakeRfcommLink(writeDelayMs = 40)
        val transport = transport(link)
        val first = async { transport.send(byteArrayOf(1)) }
        val second = async { transport.send(byteArrayOf(2)) }
        assertEquals(PrintResult.Printed, first.await())
        assertEquals(PrintResult.Printed, second.await())
        assertEquals(1, link.maxInFlight.get())
    }

    @Test
    fun failedConnectsDoNotReportReady() = runBlocking {
        val link = FakeRfcommLink(failConnect = RfcommStrategy.entries.toSet())
        val transport = transport(link)

        val result = transport.send(byteArrayOf(1))
        assertTrue(result is PrintResult.Failed)
        assertNotEquals(PeripheralStatus.READY, transport.currentStatus)
        assertTrue(link.cancelDiscoveryCount >= 1)
    }

    @Test
    fun connectTimeoutTriesTheNextStrategy() = runBlocking {
        val link = FakeRfcommLink(blockNextConnects = 1)
        val transport = transport(link, timings = BluetoothLinkTimings.Immediate.copy(connectTimeoutMs = 150))

        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(1)))
        assertEquals(listOf(RfcommStrategy.SECURE_SPP, RfcommStrategy.INSECURE_SPP), link.opened)
        assertEquals(PeripheralStatus.READY, transport.currentStatus)
    }

    @Test
    fun blockedWriteReconnects() = runBlocking {
        val link = FakeRfcommLink()
        val transport = transport(link, timings = BluetoothLinkTimings.Immediate.copy(writeTimeoutMs = 150))

        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(1)))
        link.blockNextWrites = 1
        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(2)))
        assertEquals(2, link.opened.size)
        assertEquals(PeripheralStatus.READY, transport.currentStatus)
    }

    @Test
    fun fullFailureStartsACooldown() = runBlocking {
        val link = FakeRfcommLink(failConnect = RfcommStrategy.entries.toSet())
        var now = 1_000L
        val transport = BluetoothPrintTransport(
            link,
            preferredStrategy = { null },
            rememberStrategy = {},
            timings = BluetoothLinkTimings.Immediate.copy(failureCooldownMs = 5_000),
            nowMs = { now },
        )

        assertTrue(transport.send(byteArrayOf(1)) is PrintResult.Failed)
        assertEquals(3, link.opened.size)
        link.opened.clear()
        now = 2_000L
        assertEquals(PrintResult.Failed(PrintFailure.TIMEOUT), transport.send(byteArrayOf(1)))
        assertEquals(0, link.opened.size)
        now = 8_000L
        assertTrue(transport.send(byteArrayOf(1)) is PrintResult.Failed)
        assertEquals(3, link.opened.size)
    }

    @Test
    fun paperOutKeepsTheSocketAndSkipsTheTicket() = runBlocking {
        val link = FakeRfcommLink(paperOut = true)
        val transport = transport(link, timings = BluetoothLinkTimings.Immediate.copy(paperProbeMs = 20))

        assertEquals(PrintResult.Failed(PrintFailure.PAPER_EMPTY), transport.send(byteArrayOf(0x1B)))
        assertEquals(PeripheralStatus.ERROR, transport.currentStatus)
        assertEquals(byteArrayOf(0x10, 0x04, 0x04).toList(), link.written.toByteArray().toList())
        assertEquals(PrintResult.Failed(PrintFailure.PAPER_EMPTY), transport.send(byteArrayOf(0x1B)))
        assertEquals(1, link.opened.size)
    }

    @Test
    fun linkUpDoesNotClaimReady() = runBlocking {
        val link = FakeRfcommLink()
        val transport = transport(link)

        transport.noteLinked()
        assertEquals(PeripheralStatus.DISCOVERED, transport.currentStatus)
        assertEquals(PrintResult.Printed, transport.send(byteArrayOf(1)))
        transport.noteLinked()
        assertEquals(PeripheralStatus.READY, transport.currentStatus)
    }

    @Test
    fun unlinkDuringConnectDoesNotFinishReady() = runBlocking {
        val started = CountDownLatch(1)
        val link = FakeRfcommLink(blockNextConnects = 1, onConnectBlocked = { started.countDown() })
        val transport = transport(link, timings = BluetoothLinkTimings.Immediate.copy(connectTimeoutMs = 2_000))
        val pending = async(Dispatchers.IO) { transport.send(byteArrayOf(1)) }

        assertTrue(started.await(2, TimeUnit.SECONDS))
        transport.noteUnlinked()
        assertEquals(PrintResult.Failed(PrintFailure.DISCONNECTED), pending.await())
        assertEquals(PeripheralStatus.DISCONNECTED, transport.currentStatus)
        assertEquals(1, link.opened.size)
    }

    @Test
    fun radioFailureStaysDisconnected() = runBlocking {
        val link = FakeRfcommLink(radioFailure = PrintFailure.DISCONNECTED)
        val transport = transport(link)

        assertEquals(PrintResult.Failed(PrintFailure.DISCONNECTED), transport.send(byteArrayOf(1)))
        assertEquals(PeripheralStatus.DISCONNECTED, transport.currentStatus)
        assertEquals(0, link.cancelDiscoveryCount)
    }

    private fun transport(
        link: FakeRfcommLink,
        preferred: () -> RfcommStrategy? = { null },
        remember: (RfcommStrategy) -> Unit = {},
        timings: BluetoothLinkTimings = BluetoothLinkTimings.Immediate,
    ) = BluetoothPrintTransport(link, preferred, remember, timings)
}

private class FakeRfcommLink(
    private val radioFailure: PrintFailure? = null,
    private val failConnect: Set<RfcommStrategy> = emptySet(),
    private val writeDelayMs: Long = 0,
    var blockNextConnects: Int = 0,
    var blockNextWrites: Int = 0,
    private val paperOut: Boolean = false,
    private val onConnectBlocked: () -> Unit = {},
    var failNextWrites: Int = 0,
) : BluetoothRfcommLink {
    val opened = mutableListOf<RfcommStrategy>()
    val written = ByteArrayOutputStream()
    var writeCount = 0
    var cancelDiscoveryCount = 0
    val maxInFlight = AtomicInteger(0)
    private val inFlight = AtomicInteger(0)

    override fun checkRadio(): PrintFailure? = radioFailure

    override fun cancelDiscovery() {
        cancelDiscoveryCount++
    }

    override fun open(strategy: RfcommStrategy): BluetoothRfcommSession {
        opened += strategy
        return FakeSession()
    }

    private inner class FakeSession : BluetoothRfcommSession {
        @Volatile var closed = false
        private val paper = object : InputStream() {
            @Volatile private var armed = false
            @Volatile private var consumed = false

            fun arm() {
                consumed = false
                armed = true
            }

            override fun available(): Int = if (armed && !consumed) 1 else 0

            override fun read(): Int {
                if (!armed || consumed) return -1
                consumed = true
                return 0x60
            }
        }
        private val output = object : OutputStream() {
            override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

            override fun write(buffer: ByteArray, offset: Int, count: Int) {
                enter()
                try {
                    if (blockNextWrites > 0) {
                        blockNextWrites--
                        while (!closed) Thread.sleep(5)
                        throw IOException("write blocked")
                    }
                    if (failNextWrites > 0) {
                        failNextWrites--
                        throw IOException("write failed")
                    }
                    if (writeDelayMs > 0) Thread.sleep(writeDelayMs)
                    written.write(buffer, offset, count)
                    writeCount++
                    if (paperOut && count >= 3 && buffer[offset] == 0x10.toByte() && buffer[offset + 1] == 0x04.toByte()) {
                        paper.arm()
                    }
                } finally {
                    leave()
                }
            }

            override fun flush() = Unit
        }

        override fun connect() {
            enter()
            try {
                if (blockNextConnects > 0) {
                    blockNextConnects--
                    onConnectBlocked()
                    while (!closed) Thread.sleep(5)
                    throw IOException("connect blocked")
                }
                val strategy = opened.last()
                if (strategy in failConnect) throw IOException("connect failed")
            } finally {
                leave()
            }
        }

        override val outputStream: OutputStream = output
        override val inputStream: InputStream = if (paperOut) paper else ByteArrayInputStream(byteArrayOf())
        override fun close() {
            closed = true
        }
    }

    private fun enter() {
        val active = inFlight.incrementAndGet()
        maxInFlight.updateAndGet { current -> maxOf(current, active) }
    }

    private fun leave() {
        inFlight.decrementAndGet()
    }
}

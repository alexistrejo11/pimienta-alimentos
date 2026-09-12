package io.github.alexistrejo.pimienta.pos.hardware

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// Provides deterministic scanner input for tests and the sandbox mode.
class FakeBarcodeScanner : BarcodeScanner {
    private val _status = MutableStateFlow(PeripheralStatus.READY)
    private val _events = MutableSharedFlow<BarcodeRead>(replay = 1, extraBufferCapacity = 16)

    override val status: Flow<PeripheralStatus> = _status.asStateFlow()
    override val events: Flow<BarcodeRead> = _events.asSharedFlow()

    // Injects one complete barcode into the scanner stream.
    fun emit(rawValue: String) {
        require(rawValue.isNotBlank()) { "Barcode cannot be blank" }
        _events.tryEmit(BarcodeRead(rawValue.trim(), ScannerSource.FAKE))
    }

    // Simulates a scanner connection change.
    fun setStatus(status: PeripheralStatus) {
        _status.value = status
    }

    override suspend fun start() { _status.value = PeripheralStatus.READY }
    override suspend fun stop() { _status.value = PeripheralStatus.DISCONNECTED }
}

// Provides deterministic printer outcomes without requiring physical hardware.
class FakeTicketPrinter(
    override val profile: PrinterProfile = PrinterProfiles.genericEscPos58,
) : TicketPrinter {
    private val _status = MutableStateFlow(PeripheralStatus.READY)
    private val _printed = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)
    var nextResult: PrintResult = PrintResult.Printed
    val printed: Flow<ByteArray> = _printed.asSharedFlow()
    override val status: Flow<PeripheralStatus> = _status.asStateFlow()

    // Records bytes when the configured result allows a print to complete.
    override suspend fun print(bytes: ByteArray): PrintResult {
        if (_status.value != PeripheralStatus.READY) return PrintResult.Failed(PrintFailure.DISCONNECTED)
        val result = nextResult
        if (result is PrintResult.Printed) _printed.tryEmit(bytes.copyOf())
        return result
    }

    // Simulates a printer connection change.
    fun setStatus(status: PeripheralStatus) {
        _status.value = status
    }
}

// Provides deterministic drawer outcomes for tests.
class FakeCashDrawer : CashDrawer {
    private val _status = MutableStateFlow(PeripheralStatus.READY)
    override val status: Flow<PeripheralStatus> = _status.asStateFlow()
    var nextResult: PrintResult = PrintResult.Printed

    override suspend fun open(): PrintResult = if (_status.value == PeripheralStatus.READY) nextResult else PrintResult.Failed(PrintFailure.DISCONNECTED)
}

// Keeps the initial generic capability profile in one place.
object PrinterProfiles {
    val genericEscPos58 = PrinterProfile(
        id = "generic-escpos-58",
        displayName = "ESC/POS genérica · 58 mm",
        supportsCut = true,
    )

    val pos5890A = PrinterProfile(
        id = "pos-5890a",
        displayName = "POS-5890A / ZJ-5890A · 58 mm",
        supportsCut = true,
        supportsCashDrawer = true,
        drawerPulse = DrawerPulse(onTime = 25, offTime = 250),
    )
}

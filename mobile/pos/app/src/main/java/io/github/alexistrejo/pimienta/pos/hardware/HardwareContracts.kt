package io.github.alexistrejo.pimienta.pos.hardware

import kotlinx.coroutines.flow.Flow
import java.time.Instant

// Describes where a normalized barcode reading came from.
enum class ScannerSource { USB_SERIAL, USB_HID, BLUETOOTH_SERIAL, FAKE }

// Carries one complete barcode without applying catalog or sales rules.
data class BarcodeRead(val rawValue: String, val source: ScannerSource, val receivedAt: Instant = Instant.now())

// Reports the lifecycle state of a peripheral or transport.
enum class PeripheralStatus { DISCONNECTED, DISCOVERED, PERMISSION_REQUIRED, READY, BUSY, ERROR, UNSUPPORTED }

// Exposes normalized scanner events to the application layer.
interface BarcodeScanner {
    val status: Flow<PeripheralStatus>
    val events: Flow<BarcodeRead>
    suspend fun start()
    suspend fun stop()
}

// Declares printer capabilities without coupling the renderer to a model.
data class PrinterProfile(
    val id: String,
    val displayName: String,
    val paperWidth: PaperWidth = PaperWidth.MM_58,
    val codePage: PrinterCodePage = PrinterCodePage.CP850,
    val supportsCut: Boolean = false,
    val supportsCashDrawer: Boolean = false,
    val drawerPulse: DrawerPulse? = null,
)

// Defines the supported physical paper widths.
enum class PaperWidth(val columns: Int) { MM_58(32) }

// Selects the byte encoding expected by a printer profile.
enum class PrinterCodePage(val escPosValue: Int, val charsetName: String) { CP850(2, "CP850") }

// Describes the pulse sent by a printer to its attached cash drawer.
data class DrawerPulse(val onTime: Int = 25, val offTime: Int = 250)

// Contains a structured print request before protocol encoding.
sealed interface PrintableDocument {
    val title: String
    val occurredAt: Instant
    val lines: List<PrintableLine>
    val totalCentavos: Long
    val paymentLabel: String?
}

// Represents one line in a printable POS document.
data class PrintableLine(
    val label: String,
    val quantity: String = "",
    val amountCentavos: Long? = null,
)

// Represents a sale or reprintable ticket document.
data class TicketDocument(
    override val title: String,
    val folio: String,
    override val occurredAt: Instant,
    override val lines: List<PrintableLine>,
    override val totalCentavos: Long,
    override val paymentLabel: String,
    val duplicate: Boolean = false,
    val siteName: String? = null,
    val siteAddress: String? = null,
    val discountCentavos: Long = 0,
    val tenderedCentavos: Long? = null,
    val changeCentavos: Long? = null,
) : PrintableDocument

// Represents a non-sale operational document such as a withdrawal or close.
data class OperationalDocument(
    override val title: String,
    val folio: String,
    override val occurredAt: Instant,
    override val lines: List<PrintableLine>,
    override val totalCentavos: Long,
    override val paymentLabel: String? = null,
) : PrintableDocument

// Describes why a print attempt did or did not complete.
sealed interface PrintResult {
    data object Printed : PrintResult
    data class Failed(val reason: PrintFailure) : PrintResult
}

// Classifies failures so the UI and retry policy can distinguish them.
enum class PrintFailure { NO_PRINTER, PERMISSION_REQUIRED, PAPER_EMPTY, DISCONNECTED, TIMEOUT, UNSUPPORTED, TRANSPORT_ERROR }

// Receives encoded ESC/POS bytes through a transport-independent contract.
interface TicketPrinter {
    val profile: PrinterProfile
    val status: Flow<PeripheralStatus>
    suspend fun print(bytes: ByteArray): PrintResult
}

// Represents optional drawer support exposed through a printer or dedicated adapter.
interface CashDrawer {
    val status: Flow<PeripheralStatus>
    suspend fun open(): PrintResult
}

// Sends bytes without knowing the structure of a ticket.
interface PrintTransport {
    val status: Flow<PeripheralStatus>
    suspend fun send(bytes: ByteArray): PrintResult
}

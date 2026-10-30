package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.alexistrejo.pimienta.pos.data.local.entity.CashCountAttemptEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.app.PosApplication
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.sync.PosApiUserMessages
import io.github.alexistrejo.pimienta.pos.data.sync.ProvisioningRepository
import io.github.alexistrejo.pimienta.pos.hardware.PosScannerRegistry
import io.github.alexistrejo.pimienta.pos.data.sync.planProductEdit
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.data.sync.runForegroundSync
import io.github.alexistrejo.pimienta.pos.data.update.ApkInstallOutcome
import io.github.alexistrejo.pimienta.pos.data.update.PosAppUpdater
import io.github.alexistrejo.pimienta.pos.data.update.ReleaseCheckOutcome
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.domain.DashboardSummary
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.domain.ShiftCloseBreakdown
import io.github.alexistrejo.pimienta.pos.domain.ShiftCloseCalculator
import io.github.alexistrejo.pimienta.pos.domain.sortedCategoryNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Tracks the blind-count workflow before a shift is sealed.
private enum class CountStage { OPEN, COUNTING, VALIDATION, COMPLETED }

// Handles the operational cash summary, blind count, rejection, and final approval.
@Composable
internal fun ZClosePanel(
    shift: ShiftEntity,
    manager: LocalUserEntity,
    users: List<LocalUserEntity>,
    summary: DashboardSummary?,
    repository: PosRepository,
    refresh: () -> Unit,
    onShiftClosed: () -> Unit,
    modifier: Modifier
) {
    var zCloseOpen by rememberSaveable(shift.id) { mutableStateOf(false) }
    var withdrawalsOpen by rememberSaveable(shift.id) { mutableStateOf(false) }
    var close by remember(shift.id) { mutableStateOf<ShiftCloseBreakdown?>(null) }

    LaunchedEffect(shift.id, summary) {
        close = withContext(Dispatchers.IO) { repository.shiftCloseBreakdown(shift) }
    }

    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Caja y Corte de Caja", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Resumen del turno activo (${shift.id.take(4).uppercase()}). El desglose de arqueo y mix comercial se muestra al validar el corte, después del conteo ciego.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val shiftSummary = close
            if (shiftSummary == null) {
                Text("Cargando resumen del turno…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                MetricTiles(
                    listOf(
                        "Ventas netas" to Money.format(shiftSummary.netCentavos),
                        "Ventas brutas" to Money.format(shiftSummary.grossCentavos),
                        "Descuentos / cortesías" to Money.format(shiftSummary.discountsCentavos),
                        "Tickets" to shiftSummary.ticketCount.toString(),
                        "Efectivo cobrado" to Money.format(shiftSummary.cashSalesCentavos),
                        "Tarjeta" to Money.format(shiftSummary.cardSalesCentavos),
                        "Sangrías" to "${shiftSummary.withdrawalCount} · ${Money.format(shiftSummary.withdrawalsCentavos)}",
                        "Ventas canceladas" to shiftSummary.cancelledCount.toString(),
                        "Efectivo teórico" to Money.format(shiftSummary.expectedCashCentavos),
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PosButton("Ver sangrías del turno", { withdrawalsOpen = true }, modifier = Modifier.weight(1f))
                PosButton("Iniciar Corte de Caja", { zCloseOpen = true }, primary = true, modifier = Modifier.weight(1f))
            }
        }
    }

    if (zCloseOpen) {
        ZCloseDialog(
            shift = shift,
            manager = manager,
            users = users,
            close = close,
            repository = repository,
            onDismiss = { zCloseOpen = false },
            onApprovedAndClosed = {
                zCloseOpen = false
                refresh()
                onShiftClosed()
            }
        )
    }

    if (withdrawalsOpen) {
        Dialog(onDismissRequest = { withdrawalsOpen = false }) {
            Surface(Modifier.widthIn(max = 720.dp), color = MaterialTheme.colorScheme.surface) {
                WithdrawalsPanel(shift, repository, Modifier.fillMaxWidth().padding(8.dp))
            }
        }
    }
}

// Shows the cash-drawer formula only after a blind count is submitted.
@Composable
private fun ShiftCashBreakdown(close: ShiftCloseBreakdown) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Arqueo de efectivo", style = MaterialTheme.typography.titleMedium)
        BreakdownLine("Fondo inicial", close.openingCashCentavos)
        BreakdownLine("(+) Efectivo cobrado", close.cashSalesCentavos)
        if (close.cancelledCount > 0) {
            BreakdownLine("Canceladas efectivo (${close.cancelledCount})", close.cancelledCashCentavos, alreadyExcluded = true)
        }
        BreakdownLine("(-) Sangrías (${close.withdrawalCount})", close.withdrawalsCentavos)
        BreakdownLine("(=) Efectivo esperado", close.expectedCashCentavos, emphasized = true)
    }
}

// Shows the commercial mix only on Corte Z validation, not while preparing the count.
@Composable
private fun ShiftCommercialBreakdown(close: ShiftCloseBreakdown) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Resumen comercial del turno", style = MaterialTheme.typography.titleMedium)
            BreakdownLine("Venta bruta (${close.ticketCount} tickets)", close.grossCentavos)
            BreakdownLine("(-) Descuentos / cortesías", close.discountsCentavos)
            BreakdownLine("(=) Venta neta", close.netCentavos, emphasized = true)
            BreakdownLine("Efectivo", close.cashSalesCentavos)
            BreakdownLine("Tarjeta", close.cardSalesCentavos)
            BreakdownLine("Cortesías (bruto regalado)", close.courtesyGrossCentavos)
            BreakdownLine("Catálogo", close.catalogCentavos)
            BreakdownLine("Monto abierto (${close.openAmountQuantity})", close.openAmountCentavos)
            BreakdownLine("Sin catalogar (${close.pendingCatalogQuantity})", close.pendingCatalogCentavos)
        }
    }
}

@Composable
private fun BreakdownLine(label: String, amount: Long, emphasized: Boolean = false, alreadyExcluded: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            if (alreadyExcluded) "$label · ya fuera del esperado" else label,
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            Money.format(amount),
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// Displays the blind count and manager approval workflow in a modal dialog.
@Composable
private fun ZCloseDialog(
    shift: ShiftEntity,
    manager: LocalUserEntity,
    users: List<LocalUserEntity>,
    close: ShiftCloseBreakdown?,
    repository: PosRepository,
    onDismiss: () -> Unit,
    onApprovedAndClosed: () -> Unit,
) {
    val context = LocalContext.current
    var stage by rememberSaveable(shift.id) { mutableStateOf(CountStage.COUNTING) }
    var count by rememberSaveable(shift.id) { mutableStateOf("") }
    var attemptId by rememberSaveable(shift.id) { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf<CashCountAttemptEntity?>(null) }
    var rejectionReason by rememberSaveable(shift.id) { mutableStateOf("") }
    val (rejectionReasonInteraction, forceRejectionReasonKeyboard) = rememberForceSoftKeyboardInteractionSource()
    var message by rememberSaveable(shift.id) { mutableStateOf<String?>(null) }
    var pinRequested by rememberSaveable(shift.id) { mutableStateOf(false) }
    var printSummaryTicket by rememberSaveable { mutableStateOf(true) }
    var liveClose by remember(shift.id) { mutableStateOf(close) }
    var autoExitSeconds by rememberSaveable(shift.id) { mutableIntStateOf(5) }
    // Blocks repeated taps on count submission and close approval while one is still saving.
    var working by remember(shift.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(shift.id, stage, attemptId) {
        liveClose = withContext(Dispatchers.IO) { repository.shiftCloseBreakdown(shift) }
        if (attempt == null && (attemptId != null || stage != CountStage.COUNTING)) {
            attempt = withContext(Dispatchers.IO) {
                val attempts = repository.cashCounts(shift.id)
                if (attemptId != null) attempts.firstOrNull { it.id == attemptId } else attempts.firstOrNull()
            }
        }
    }

    // Auto-exits to shift opening after sealing the shift in CountStage.COMPLETED
    LaunchedEffect(stage) {
        if (stage == CountStage.COMPLETED) {
            while (autoExitSeconds > 0) {
                delay(1000)
                autoExitSeconds--
            }
            onApprovedAndClosed()
        }
    }

    fun submit() {
        if (working) return
        val amount = Money.fromInput(count)
        if (amount == null || amount < 0) {
            message = "Captura un conteo válido."
        } else {
            working = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { repository.submitCashCount(shift, amount, "total=$amount") }
                working = false
                if (result == null) {
                    message = "No se pudo guardar el conteo local."
                } else {
                    attempt = result
                    attemptId = result.id
                    SyncWorker.enqueue(context)
                    stage = CountStage.VALIDATION
                }
            }
        }
    }

    Dialog(onDismissRequest = { /* Prevent accidental dismiss on backdrop tap for security */ }) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (stage) {
                    CountStage.OPEN, CountStage.COUNTING -> {
                        Text("Conteo ciego de caja", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Captura el efectivo físico en cajón. No se muestra el monto esperado ni la diferencia.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Numpad(count, { count = it }, decimal = true)
                        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                            PosButton("Enviar a validación", ::submit, enabled = !working, primary = true, modifier = Modifier.weight(1f))
                        }
                    }
                    CountStage.VALIDATION -> {
                        val counted = attempt?.totalCentavos ?: 0
                        val expected = liveClose?.expectedCashCentavos ?: 0L
                        Text("Validar Corte de Caja · ${manager.displayName}", style = MaterialTheme.typography.titleLarge)
                        liveClose?.let {
                            ShiftCommercialBreakdown(it)
                            ShiftCashBreakdown(it)
                        }
                        Text("Conteo físico: ${Money.format(counted)}")
                        Text(
                            "Diferencia: ${Money.format(ShiftCloseCalculator.differenceCentavos(counted, expected))}",
                            fontWeight = FontWeight.Bold,
                        )
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (it.isFocused) forceRejectionReasonKeyboard() },
                            interactionSource = rejectionReasonInteraction,
                            label = { Text("Motivo si se devuelve a corrección") },
                            colors = formFieldColors(),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { printSummaryTicket = !printSummaryTicket }
                        ) {
                            Checkbox(checked = printSummaryTicket, onCheckedChange = { printSummaryTicket = it })
                            Spacer(Modifier.width(6.dp))
                            Text("Imprimir ticket comprobante de Corte de Caja", style = MaterialTheme.typography.bodyMedium)
                        }
                        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PosButton(
                                "Devolver a corrección",
                                {
                                    if (rejectionReason.isBlank()) message = "Escribe un motivo de corrección."
                                    else scope.launch {
                                        withContext(Dispatchers.IO) { attempt?.let { repository.rejectCashCount(it.id, rejectionReason) } }
                                        attempt = null
                                        attemptId = null
                                        count = ""
                                        rejectionReason = ""
                                        message = "Conteo devuelto a corrección. Ingresa el nuevo conteo físico."
                                        stage = CountStage.COUNTING
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PosButton("Aprobar con PIN", { pinRequested = true }, primary = true, modifier = Modifier.weight(1f))
                        }
                    }
                    CountStage.COMPLETED -> {
                        val counted = attempt?.totalCentavos ?: 0
                        val expected = liveClose?.expectedCashCentavos ?: 0L
                        val diff = ShiftCloseCalculator.differenceCentavos(counted, expected)

                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "✓ Corte de Caja Completado",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    "El turno ha sido sellado correctamente en la base de datos local.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }

                        Text("Resumen del Corte Z", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Conteo físico en caja: ${Money.format(counted)}")
                        Text("Efectivo teórico esperado: ${Money.format(expected)}")
                        Text(
                            "Diferencia final: ${Money.format(diff)}",
                            fontWeight = FontWeight.Bold,
                            color = if (diff == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )

                        if (printSummaryTicket) {
                            Text(
                                "Se ha enviado el ticket comprobante de Corte de Caja a la cola de impresión.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Text(
                            if (autoExitSeconds > 0) "Saliendo automáticamente a la pantalla de inicio en ${autoExitSeconds}s..."
                            else "Regresando a la pantalla de inicio...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        PosButton(
                            label = if (autoExitSeconds > 0) "Finalizar y salir al inicio (${autoExitSeconds}s) →" else "Finalizando...",
                            click = {
                                onApprovedAndClosed()
                            },
                            primary = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (pinRequested && attempt != null) {
        val currentAttempt = attempt ?: return
        ManagerPinDialog(
            users = users,
            title = "Firmar y cerrar turno",
            initialManager = manager,
            repository = repository,
            onDismiss = { pinRequested = false },
        ) { signingManager, pin ->
            if (working) return@ManagerPinDialog
            working = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { repository.approveShiftClose(shift, currentAttempt, signingManager, pin, printSummaryTicket) }
                working = false
                pinRequested = false
                result.onSuccess {
                    PrintWorker.enqueue(context)
                    SyncWorker.enqueue(context)
                    stage = CountStage.COMPLETED
                }.onFailure { ex ->
                    message = ex.message ?: "No se pudo cerrar el turno."
                }
            }
        }
    }
}

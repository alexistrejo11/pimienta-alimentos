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
// Displays real tickets from the current shift and queues reprints locally.
@Composable
internal fun HistoryPanel(shift: ShiftEntity, manager: LocalUserEntity, repository: PosRepository, refresh: () -> Unit, modifier: Modifier) {
    var sales by remember(shift.id) { mutableStateOf<List<SaleEntity>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    val (queryInteraction, forceQueryKeyboard) = rememberForceSoftKeyboardInteractionSource()
    var pageSize by rememberSaveable(query) { mutableIntStateOf(10) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var cancelSaleId by rememberSaveable { mutableStateOf<String?>(null) }
    val cancelSale = remember(cancelSaleId, sales) { sales.firstOrNull { it.id == cancelSaleId } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(shift.id) { sales = withContext(Dispatchers.IO) { repository.salesForShift(shift.id) } }

    val filtered = remember(sales, query) {
        sales.filter { query.isBlank() || it.folio.contains(query, ignoreCase = true) }
    }
    val visibleSales = remember(filtered, pageSize) { filtered.take(pageSize) }

    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Historial del turno", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Historial exclusivo de ventas del turno activo (${shift.id.take(4).uppercase()}). Muestra los tickets generados durante este turno.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) forceQueryKeyboard() },
                interactionSource = queryInteraction,
                label = { Text("Buscar por folio (búsqueda parcial)") },
                placeholder = { Text("Ej. 0001") },
                singleLine = true,
                colors = catalogFieldColors(),
            )

            if (filtered.isEmpty()) {
                EmptySurface("No se encontraron tickets en este turno para la búsqueda.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(visibleSales, key = { it.id }) { sale ->
                        SaleHistoryRow(sale, repository, { message = it; refresh() }, { cancelSaleId = sale.id })
                    }
                    if (filtered.size > visibleSales.size) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            PosButton(
                                label = "Mostrar más (${visibleSales.size} de ${filtered.size})",
                                click = { pageSize += 10 },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    cancelSale?.let { sale ->
        CancellationDialog(
            sale = sale,
            manager = manager,
            repository = repository,
            onDismiss = { cancelSaleId = null },
            onDone = { notice -> message = notice; cancelSaleId = null; scope.launch { sales = withContext(Dispatchers.IO) { repository.salesForShift(shift.id) }; refresh() } },
        )
    }
}

// Renders one sale row and exposes the allowed local reprint action.
@Composable
private fun SaleHistoryRow(sale: SaleEntity, repository: PosRepository, notice: (String) -> Unit, cancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sale.folio, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(Money.format(sale.totalCentavos), fontWeight = FontWeight.Bold)
            }
            Text("${sale.paymentMethod} · ${if (sale.status == "CANCELLED") "CANCELADA" else "CONFIRMADA"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosButton("Reimprimir", { scope.launch { withContext(Dispatchers.IO) { repository.requestReprint(sale.id) }; PrintWorker.enqueue(context); notice("Reimpresión enviada a la impresora o a la cola local.") } }, modifier = Modifier.weight(1f))
                if (sale.paymentMethod == "CASH" && sale.status != "CANCELLED") PosButton("Cancelar efectivo", cancel, modifier = Modifier.weight(1f))
            }
        }
    }
}

// Captures the required reason and local Manager signature for a cash cancellation.
@Composable
private fun CancellationDialog(sale: SaleEntity, manager: LocalUserEntity, repository: PosRepository, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    val (reasonInteraction, forceReasonKeyboard) = rememberForceSoftKeyboardInteractionSource()
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    // Blocks a second confirmation while the first cancellation is still being saved.
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Cancelar venta en efectivo", style = MaterialTheme.typography.titleLarge)
                Text("El ticket se conserva como cancelado y se revierte el movimiento de inventario.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) forceReasonKeyboard() },
                    interactionSource = reasonInteraction,
                    label = { Text("Motivo obligatorio") },
                    singleLine = true,
                    colors = catalogFieldColors(),
                )
                Text("PIN de ${manager.displayName}", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cerrar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Confirmar cancelación", {
                        if (working) return@PosButton
                        if (reason.isBlank() || pin.length < 4) {
                            message = "Captura motivo y PIN de cuatro dígitos."
                        } else {
                            working = true
                            scope.launch {
                                val validPin = withContext(Dispatchers.IO) { repository.authenticate(manager.id, pin) }
                                if (!validPin) {
                                    message = "PIN de ${manager.displayName} incorrecto."
                                    pin = ""
                                    working = false
                                    return@launch
                                }
                                val ok = withContext(Dispatchers.IO) { repository.cancelCashSale(sale, manager, pin, reason) }
                                if (ok) {
                                    SyncWorker.enqueue(context)
                                    onDone("Venta ${sale.folio} cancelada y auditada.")
                                } else {
                                    message = "No se pudo cancelar la venta."
                                    working = false
                                }
                            }
                        }
                    }, enabled = !working, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


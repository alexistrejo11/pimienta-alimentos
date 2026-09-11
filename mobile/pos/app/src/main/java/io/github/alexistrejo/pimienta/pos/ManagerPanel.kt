package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.alexistrejo.pimienta.pos.data.local.entity.CashCountAttemptEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.InventoryMovementEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PrintJobEntity
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.domain.DashboardSummary
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Describes the local Manager workspace navigation.
private enum class ManagerSection(val label: String) { DASHBOARD("Resumen del día"), Z_CLOSE("Caja y Corte Z"), INVENTORY("Inventario"), HISTORY("Historial"), STATUS("Estado") }
// Tracks the blind-count workflow before a shift is sealed.
private enum class CountStage { OPEN, COUNTING, VALIDATION }

// Requests Manager authorization without changing the cashier session.
@Composable
internal fun ManagerAccess(users: List<LocalUserEntity>, repository: PosRepository, onDismiss: () -> Unit, onAuthorized: (LocalUserEntity) -> Unit) {
    val managers = users.filter { it.role == "MANAGER" || it.role == "SUPERADMIN" }
    var selected by remember { mutableStateOf(managers.firstOrNull()) }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun authorize() {
        val user = selected ?: return
        scope.launch { if (withContext(Dispatchers.IO) { repository.authenticate(user.id, pin) }) onAuthorized(user) else message = "El PIN no corresponde al perfil seleccionado." }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.widthIn(max = 520.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Autorizar acceso a Manager", style = MaterialTheme.typography.titleLarge)
                Text("La venta y el carrito permanecerán activos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                managers.forEach { user -> PosButton(user.displayName, { selected = user }, selected = selected?.id == user.id, modifier = Modifier.fillMaxWidth()) }
                Text("PIN de Manager", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true, onSubmit = ::authorize)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f)); PosButton("Autorizar", ::authorize, primary = true, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

// Shows the local dashboard when no shift is open; operational actions stay unavailable.
@Composable
internal fun ManagerReadOnlyPanel(manager: LocalUserEntity, repository: PosRepository, onExit: () -> Unit) {
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    LaunchedEffect(Unit) { summary = withContext(Dispatchers.IO) { repository.dailySummary() } }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ManagerHeaderWithoutShift(manager, onExit)
        DashboardPanel(summary, summary?.pendingEvents ?: 0, Modifier.weight(1f))
    }
}

// Makes the no-shift state explicit so nobody can mistake the dashboard for an open register.
@Composable
private fun ManagerHeaderWithoutShift(manager: LocalUserEntity, onExit: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        PosButton("Volver", onExit)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Panel de control", style = MaterialTheme.typography.titleLarge)
            Text("Solo lectura · no hay turno activo · ${manager.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Caja cerrada", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Renders the Manager workspace with a visual dashboard and local Room-backed sections.
@Composable
internal fun ManagerPanel(shift: ShiftEntity, manager: LocalUserEntity, products: List<ProductEntity>, pendingEvents: Int, repository: PosRepository, onReturnToSale: () -> Unit, onShiftClosed: () -> Unit = {}) {
    var section by remember { mutableStateOf(ManagerSection.DASHBOARD) }
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var webCentralMessage by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(shift.id, refreshToken) { summary = withContext(Dispatchers.IO) { repository.dailySummary() } }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val landscape = maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            ManagerHeader(shift, manager, onReturnToSale) { webCentralMessage = "La URL de Web Central se configurará con el entorno de la sede; las operaciones locales siguen disponibles." }
            webCentralMessage?.let { Text(it, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (landscape) Row(Modifier.weight(1f).fillMaxWidth()) {
                ManagerSideNav(section, { section = it }, Modifier.width(188.dp).fillMaxHeight())
                HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                ManagerSectionContent(section, shift, manager, products, pendingEvents, summary, repository, { refreshToken++ }, onShiftClosed, Modifier.weight(1f))
            } else {
                ManagerCompactNav(section, { section = it })
                ManagerSectionContent(section, shift, manager, products, pendingEvents, summary, repository, { refreshToken++ }, onShiftClosed, Modifier.weight(1f))
            }
        }
    }
}

// Keeps return and Web Central shortcuts visible in every section.
@Composable
private fun ManagerHeader(shift: ShiftEntity, manager: LocalUserEntity, onReturn: () -> Unit, onOpenWebCentral: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        PosButton("Volver a caja", onReturn)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text("Panel de control", style = MaterialTheme.typography.titleLarge); Text("Resumen local · Tablet T1 · Turno ${shift.id.take(4).uppercase()} · ${manager.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        PosButton("Abrir Web Central", onOpenWebCentral)
    }
}

// Provides persistent landscape navigation.
@Composable
private fun ManagerSideNav(selected: ManagerSection, choose: (ManagerSection) -> Unit, modifier: Modifier) { Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { ManagerSection.entries.forEach { item -> PosButton(item.label, { choose(item) }, selected = selected == item, modifier = Modifier.fillMaxWidth()) } } }

// Uses a compact selector in portrait.
@Composable
private fun ManagerCompactNav(selected: ManagerSection, choose: (ManagerSection) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        PosButton("Sección: ${selected.label}", { expanded = true }, modifier = Modifier.fillMaxWidth())
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { ManagerSection.entries.forEach { item -> DropdownMenuItem(text = { Text(item.label) }, onClick = { choose(item); expanded = false }) } }
    }
}

// Routes each Manager area while preserving the local session.
@Composable
private fun ManagerSectionContent(section: ManagerSection, shift: ShiftEntity, manager: LocalUserEntity, products: List<ProductEntity>, pendingEvents: Int, summary: DashboardSummary?, repository: PosRepository, refresh: () -> Unit, onShiftClosed: () -> Unit, modifier: Modifier) {
    when (section) {
        ManagerSection.DASHBOARD -> DashboardPanel(summary, pendingEvents, modifier)
        ManagerSection.Z_CLOSE -> ZClosePanel(shift, manager, summary, repository, refresh, onShiftClosed, modifier)
        ManagerSection.INVENTORY -> InventoryPanel(shift, manager, products, repository, refresh, modifier)
        ManagerSection.HISTORY -> HistoryPanel(shift, manager, repository, refresh, modifier)
        ManagerSection.STATUS -> StatusPanel(pendingEvents, repository, modifier)
    }
}

// Shows daily local metrics as flat operational tiles.
@Composable
private fun DashboardPanel(summary: DashboardSummary?, pendingEvents: Int, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Resumen del día", style = MaterialTheme.typography.headlineSmall)
            Text("Actividad local de esta tablet · fecha operativa local", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (summary == null) Text("Cargando resumen local…", color = MaterialTheme.colorScheme.onSurfaceVariant) else {
                MetricGrid(summary, pendingEvents)
                Text("Productos más vendidos", style = MaterialTheme.typography.titleMedium)
                if (summary.topProducts.isEmpty()) EmptySurface("Aún no hay ventas registradas hoy.") else summary.topProducts.forEachIndexed { index, product ->
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("${index + 1}", modifier = Modifier.width(32.dp)); Text(product.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${product.quantity} · ${Money.format(product.amountCentavos)}", fontWeight = FontWeight.SemiBold) } }
                }
            }
        }
    }
}

// Renders the dashboard metric grid.
@Composable
private fun MetricGrid(summary: DashboardSummary, pendingEvents: Int) {
    val metrics = listOf("Ventas netas" to Money.format(summary.netCentavos), "Ventas brutas" to Money.format(summary.grossCentavos), "Descuentos / cortesías" to Money.format(summary.discountsCentavos), "Tickets" to summary.ticketCount.toString(), "Ticket promedio" to Money.format(summary.averageTicketCentavos), "Efectivo cobrado" to Money.format(summary.cashCollectedCentavos), "Sangrías" to "${summary.withdrawalCount} · ${Money.format(summary.withdrawalsCentavos)}", "Mermas" to summary.wasteCount.toString(), "Ventas canceladas" to summary.cancelledCount.toString(), "Pendientes sync" to pendingEvents.toString())
    metrics.chunked(3).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { (label, value) -> MetricTile(label, value, Modifier.weight(1f)) }; repeat(3 - row.size) { Spacer(Modifier.weight(1f)) } } }
}

// Creates a flat metric tile with a clear numeric hierarchy.
@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) { Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) } } }

// Handles the operational cash summary, blind count, rejection, and final approval.
@Composable
private fun ZClosePanel(shift: ShiftEntity, manager: LocalUserEntity, summary: DashboardSummary?, repository: PosRepository, refresh: () -> Unit, onShiftClosed: () -> Unit, modifier: Modifier) {
    var stage by remember(shift.id) { mutableStateOf(CountStage.OPEN) }
    var count by remember(shift.id) { mutableStateOf("") }
    var attempt by remember(shift.id) { mutableStateOf<CashCountAttemptEntity?>(null) }
    var rejectionReason by remember(shift.id) { mutableStateOf("") }
    var message by remember(shift.id) { mutableStateOf<String?>(null) }
    var pinRequested by remember(shift.id) { mutableStateOf(false) }
    var withdrawalsOpen by remember(shift.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var expected by remember(shift.id) { mutableStateOf(0L) }
    LaunchedEffect(shift.id, summary) { expected = withContext(Dispatchers.IO) { repository.expectedCash(shift) } }
    fun submit() { val amount = Money.fromInput(count); if (amount == null || amount < 0) message = "Captura un conteo válido." else scope.launch { attempt = withContext(Dispatchers.IO) { repository.submitCashCount(shift, amount, "total=$amount") }; if (attempt == null) message = "No se pudo guardar el conteo local." else stage = CountStage.VALIDATION } }
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Caja y Corte Z", style = MaterialTheme.typography.headlineSmall)
            Text("Turno ${shift.id.take(4).uppercase()} · Fondo inicial ${Money.format(shift.openingCashCentavos)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            PosButton("Ver sangrías del turno", { withdrawalsOpen = true })
            when (stage) {
                CountStage.OPEN -> { summary?.let { MetricGrid(it, 0) }; Text("Efectivo teórico en cajón: ${Money.format(expected)}", style = MaterialTheme.typography.titleLarge); PosButton("Iniciar conteo ciego", { stage = CountStage.COUNTING }, primary = true) }
                CountStage.COUNTING -> { Text("Conteo ciego · Cajero", style = MaterialTheme.typography.titleLarge); Text("No se muestra el efectivo esperado ni la diferencia.", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Total contado: ${Money.format(Money.fromInput(count) ?: 0)}", style = MaterialTheme.typography.headlineSmall); Numpad(count, { count = it }); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PosButton("Cancelar", { stage = CountStage.OPEN }, modifier = Modifier.weight(1f)); PosButton("Enviar a validación", ::submit, primary = true, modifier = Modifier.weight(1f)) } }
                CountStage.VALIDATION -> { val counted = attempt?.totalCentavos ?: 0; Text("Validar Corte Z · ${manager.displayName}", style = MaterialTheme.typography.titleLarge); Text("Efectivo esperado: ${Money.format(expected)}"); Text("Conteo físico: ${Money.format(counted)}"); Text("Diferencia: ${Money.format(counted - expected)}", fontWeight = FontWeight.Bold); OutlinedTextField(rejectionReason, { rejectionReason = it }, label = { Text("Motivo si se devuelve a corrección") }, modifier = Modifier.fillMaxWidth()); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PosButton("Devolver para corregir", { if (rejectionReason.isBlank()) message = "Escribe un motivo de corrección." else scope.launch { withContext(Dispatchers.IO) { attempt?.let { repository.rejectCashCount(it.id, rejectionReason) } }; attempt = null; count = ""; rejectionReason = ""; stage = CountStage.COUNTING } }, modifier = Modifier.weight(1f)); PosButton("Aprobar con PIN", { pinRequested = true }, primary = true, modifier = Modifier.weight(1f)) } }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (pinRequested && attempt != null) ManagerPinDialog(manager, "Firmar y cerrar turno", { pinRequested = false }) { pin -> scope.launch { val closed = withContext(Dispatchers.IO) { repository.approveShiftClose(shift, attempt!!, manager, pin) }; pinRequested = false; if (closed) { refresh(); onShiftClosed() } else message = "No se pudo aprobar el Corte Z." } }
    if (withdrawalsOpen) Dialog(onDismissRequest = { withdrawalsOpen = false }) {
        Surface(Modifier.widthIn(max = 720.dp), color = MaterialTheme.colorScheme.surface) {
            WithdrawalsPanel(shift, repository, Modifier.fillMaxWidth().padding(8.dp))
        }
    }
}

// Shows immutable safeguard withdrawals in a compact read-only list.
@Composable
private fun WithdrawalsPanel(shift: ShiftEntity, repository: PosRepository, modifier: Modifier) {
    var withdrawals by remember(shift.id) { mutableStateOf<List<CashWithdrawalEntity>>(emptyList()) }
    LaunchedEffect(shift.id) { withdrawals = withContext(Dispatchers.IO) { repository.withdrawals(shift.id) } }
    val total = withdrawals.sumOf { it.amountCentavos }
    Surface(modifier, color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Sangrías de resguardo", style = MaterialTheme.typography.headlineSmall); Text("Consulta de solo lectura · se registran desde la barra de caja.", color = MaterialTheme.colorScheme.onSurfaceVariant); MetricTile("Total retirado · ${withdrawals.size} registros", Money.format(total)); if (withdrawals.isEmpty()) EmptySurface("No hay sangrías en este turno.") else withdrawals.forEach { withdrawal -> Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("${withdrawal.folio} · ${Money.format(withdrawal.amountCentavos)}", style = MaterialTheme.typography.titleMedium); Text("Cajero ${withdrawal.cashierId} · Autorizó ${withdrawal.authorizedByUserId}", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Comprobante pendiente de hardware", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } }
}

// Persists restocks and wastes while keeping the form compact for touch input.
@Composable
private fun InventoryPanel(shift: ShiftEntity, manager: LocalUserEntity, products: List<ProductEntity>, repository: PosRepository, refresh: () -> Unit, modifier: Modifier) {
    var type by remember { mutableStateOf("RESTOCK") }; var selected by remember { mutableStateOf(products.firstOrNull()) }; var quantity by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }; var message by remember { mutableStateOf<String?>(null) }; var movements by remember { mutableStateOf(emptyList<InventoryMovementEntity>()) }; val scope = rememberCoroutineScope()
    LaunchedEffect(shift.id) { movements = withContext(Dispatchers.IO) { repository.recentInventoryMovements() } }
    Surface(modifier, color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Inventario operativo", style = MaterialTheme.typography.headlineSmall); Text("Manager ${manager.displayName} · cambios locales con sincronización pendiente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PosButton("Reposición", { type = "RESTOCK" }, selected = type == "RESTOCK", modifier = Modifier.weight(1f)); PosButton("Merma", { type = "WASTE" }, selected = type == "WASTE", modifier = Modifier.weight(1f)) }
        Text("Producto", style = MaterialTheme.typography.labelLarge); products.take(8).forEach { product -> PosButton(product.name, { selected = product }, selected = selected?.id == product.id, modifier = Modifier.fillMaxWidth()) }
        Text("Cantidad: ${quantity.ifBlank { "0" }}", style = MaterialTheme.typography.titleLarge); Numpad(quantity, { quantity = it }); OutlinedTextField(reason, { reason = it }, label = { Text("Motivo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        PosButton("Registrar ${if (type == "WASTE") "merma" else "reposición"}", { val product = selected; val count = quantity.toIntOrNull(); if (product == null || count == null || count <= 0 || reason.isBlank()) message = "Selecciona producto, cantidad y motivo." else scope.launch { val saved = withContext(Dispatchers.IO) { repository.recordInventoryMovement(shift, product, count, type, reason) }; message = if (saved) "Movimiento guardado localmente." else "No se pudo guardar el movimiento."; if (saved) { quantity = ""; reason = ""; movements = withContext(Dispatchers.IO) { repository.recentInventoryMovements() }; refresh() } } }, primary = true, modifier = Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("Movimientos recientes", style = MaterialTheme.typography.titleMedium)
        if (movements.isEmpty()) EmptySurface("Aún no hay reposiciones ni mermas registradas.") else movements.take(8).forEach { movement ->
            val productName = products.firstOrNull { it.id == movement.productId }?.name ?: "Producto local"
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(productName, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${movementTypeLabel(movement.movementType)} · ${movement.quantityDelta}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("Crear productos, precios y disponibilidad se gestiona en Web Central.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    } }
}

// Converts persisted movement codes into cashier-facing Spanish labels.
private fun movementTypeLabel(type: String): String = when (type) {
    "WASTE" -> "Merma"
    "RESTOCK" -> "Reposición"
    "SALE_CANCELLATION" -> "Cancelación"
    "SALE" -> "Venta"
    else -> type
}

// Displays real tickets from the current shift and queues reprints locally.
@Composable
private fun HistoryPanel(shift: ShiftEntity, manager: LocalUserEntity, repository: PosRepository, refresh: () -> Unit, modifier: Modifier) {
    var sales by remember(shift.id) { mutableStateOf<List<SaleEntity>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var cancelSale by remember { mutableStateOf<SaleEntity?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(shift.id) { sales = withContext(Dispatchers.IO) { repository.salesForShift(shift.id) } }
    val filtered = sales.filter { query.isBlank() || it.folio.contains(query, true) }
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Historial del turno", style = MaterialTheme.typography.headlineSmall)
            Text("Solo tickets de este turno · Manager ${manager.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(query, { query = it }, label = { Text("Buscar por folio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { sale ->
                    SaleHistoryRow(sale, repository, { message = it; refresh() }, { cancelSale = sale })
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
            onDismiss = { cancelSale = null },
            onDone = { notice -> message = notice; cancelSale = null; scope.launch { sales = withContext(Dispatchers.IO) { repository.salesForShift(shift.id) }; refresh() } },
        )
    }
}

// Renders one sale row and exposes the allowed local reprint action.
@Composable
private fun SaleHistoryRow(sale: SaleEntity, repository: PosRepository, notice: (String) -> Unit, cancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sale.folio, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(Money.format(sale.totalCentavos), fontWeight = FontWeight.Bold)
            }
            Text("${sale.paymentMethod} · ${if (sale.status == "CANCELLED") "CANCELADA" else "CONFIRMADA"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosButton("Reimprimir", { scope.launch { withContext(Dispatchers.IO) { repository.requestReprint(sale.id) }; notice("Reimpresión agregada a la cola local.") } }, modifier = Modifier.weight(1f))
                if (sale.paymentMethod == "CASH" && sale.status != "CANCELLED") PosButton("Cancelar efectivo", cancel, modifier = Modifier.weight(1f))
            }
        }
    }
}

// Captures the required reason and local Manager signature for a cash cancellation.
@Composable
private fun CancellationDialog(sale: SaleEntity, manager: LocalUserEntity, repository: PosRepository, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.widthIn(max = 520.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cancelar venta en efectivo", style = MaterialTheme.typography.titleLarge)
                Text("El ticket se conserva como cancelado y se revierte el movimiento de inventario.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(reason, { reason = it }, label = { Text("Motivo obligatorio") }, modifier = Modifier.fillMaxWidth())
                Text("PIN de ${manager.displayName}", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cerrar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Confirmar cancelación", {
                        if (reason.isBlank() || pin.length < 4) message = "Captura motivo y PIN de cuatro dígitos."
                        else scope.launch { val ok = withContext(Dispatchers.IO) { repository.cancelCashSale(sale, manager, pin, reason) }; if (ok) onDone("Venta ${sale.folio} cancelada y auditada.") else message = "No se pudo cancelar la venta." }
                    }, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Presents durable queue state and the currently available Phase 3A peripheral runtime.
@Composable
private fun StatusPanel(pendingEvents: Int, repository: PosRepository, modifier: Modifier) {
    val printJobs = remember { mutableStateOf(emptyList<PrintJobEntity>()) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { printJobs.value = withContext(Dispatchers.IO) { repository.pendingPrintJobs() } }
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Estado y periféricos", style = MaterialTheme.typography.headlineSmall)
            MetricTile("Eventos pendientes", pendingEvents.toString())
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sincronización", style = MaterialTheme.typography.titleMedium)
                    Text("Offline-first · los eventos permanecen en Room hasta sincronizar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PosButton("Intentar sincronizar ahora", { syncMessage = "La cola local está lista; el worker sincronizará al recuperar conexión." })
                    syncMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Impresión", style = MaterialTheme.typography.titleMedium)
                    val failed = printJobs.value.count { it.status == "FAILED" }
                    Text("${printJobs.value.size} trabajos pendientes · $failed fallidos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (repository.mode() == RuntimeMode.SANDBOX) "Impresora fake lista · perfil ESC/POS 58 mm"
                        else "Sin adapter físico configurado · los trabajos permanecen pendientes",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PosButton("Procesar cola de impresión", { PrintWorker.enqueue(context) })
                    PosButton("Imprimir y abrir cajón", { }, enabled = false)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lector", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (repository.mode() == RuntimeMode.SANDBOX) "Fake scanner disponible para pruebas"
                        else "Scanner físico pendiente de validación",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PosButton("Probar lectura", { }, enabled = false)
                }
            }
        }
    }
}

// Confirms a Manager PIN for high-impact actions such as sealing a shift.
@Composable
private fun ManagerPinDialog(manager: LocalUserEntity, title: String, onDismiss: () -> Unit, onApproved: (String) -> Unit) { var pin by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; fun submit() { if (pin.length < 4) error = "Captura los cuatro dígitos del PIN." else onApproved(pin) }; Dialog(onDismissRequest = onDismiss) { Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) { Column(Modifier.widthIn(max = 480.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text("Autorizador: ${manager.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant); Numpad(pin, { pin = it }, masked = true, onSubmit = ::submit); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f)); PosButton("Firmar", ::submit, primary = true, modifier = Modifier.weight(1f)) } } } } }

// Keeps empty states explicit instead of rendering a blank surface.
@Composable
private fun EmptySurface(message: String) { Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Text(message, Modifier.fillMaxWidth().padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }

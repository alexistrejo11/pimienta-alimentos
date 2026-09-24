package io.github.alexistrejo.pimienta.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PrintJobEntity
import io.github.alexistrejo.pimienta.pos.app.PosApplication
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.sync.ProvisioningRepository
import io.github.alexistrejo.pimienta.pos.data.sync.planProductEdit
import io.github.alexistrejo.pimienta.pos.data.sync.scanCode
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.data.sync.runForegroundSync
import io.github.alexistrejo.pimienta.pos.data.update.ApkInstallOutcome
import io.github.alexistrejo.pimienta.pos.data.update.PosAppUpdater
import io.github.alexistrejo.pimienta.pos.data.update.ReleaseCheckOutcome
import io.github.alexistrejo.pimienta.pos.hardware.BondedPrinter
import io.github.alexistrejo.pimienta.pos.hardware.EscPosEncoder
import io.github.alexistrejo.pimienta.pos.hardware.PrinterLink
import io.github.alexistrejo.pimienta.pos.hardware.PrinterPreferences
import io.github.alexistrejo.pimienta.pos.hardware.bondedPrinters
import io.github.alexistrejo.pimienta.pos.hardware.OperationalDocument
import io.github.alexistrejo.pimienta.pos.hardware.PeripheralStatus
import io.github.alexistrejo.pimienta.pos.hardware.PosPrinterRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PosScannerRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PrintableLine
import io.github.alexistrejo.pimienta.pos.hardware.PrinterFactory
import io.github.alexistrejo.pimienta.pos.hardware.printerStatusPresentation
import io.github.alexistrejo.pimienta.pos.hardware.PrintResult
import java.time.Instant
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.domain.DashboardSummary
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.domain.ShiftCloseBreakdown
import io.github.alexistrejo.pimienta.pos.domain.ShiftCloseCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Describes the local Manager workspace navigation.
private enum class ManagerSection(val label: String) { DASHBOARD("Resumen del día"), Z_CLOSE("Caja y Corte de Caja"), HISTORY("Historial"), PRODUCTS("Productos"), STATUS("Estado") }
// Tracks the blind-count workflow before a shift is sealed.
private enum class CountStage { OPEN, COUNTING, VALIDATION, COMPLETED }

// Requests Manager authorization without changing the cashier session.
@Composable
internal fun ManagerAccess(
    users: List<LocalUserEntity>,
    repository: PosRepository,
    onDismiss: () -> Unit,
    onAuthorized: (LocalUserEntity) -> Unit
) {
    val managers = remember(users) { users.filter { it.active && it.isManagerOrAdmin } }
    if (managers.isEmpty()) {
        val fallback = users.firstOrNull() ?: LocalUserEntity(id = "manager", displayName = "Gerente", role = "MANAGER", pinHash = "", active = true)
        LaunchedEffect(Unit) { onAuthorized(fallback) }
    } else {
        ManagerPinDialog(
            users = users,
            title = "Autorizar acceso a Manager",
            repository = repository,
            onDismiss = onDismiss,
            onApproved = { manager, _ -> onAuthorized(manager) },
        )
    }
}

// Shows the local dashboard when no shift is open; operational actions stay unavailable.
// Renders the Manager workspace with a visual dashboard and local Room-backed sections.
@Composable
internal fun ManagerPanel(
    shift: ShiftEntity? = null,
    manager: LocalUserEntity,
    users: List<LocalUserEntity> = emptyList(),
    products: List<ProductEntity>,
    pendingEvents: Int,
    repository: PosRepository,
    onReturnToSale: () -> Unit,
    onShiftClosed: () -> Unit = {}
) {
    val availableSections = remember(shift) {
        if (shift != null) {
            ManagerSection.entries
        } else {
            listOf(ManagerSection.DASHBOARD, ManagerSection.PRODUCTS, ManagerSection.STATUS)
        }
    }
    var section by rememberSaveable { mutableStateOf(ManagerSection.DASHBOARD) }

    LaunchedEffect(shift) {
        if (shift == null && (section == ManagerSection.Z_CLOSE || section == ManagerSection.HISTORY)) {
            section = ManagerSection.DASHBOARD
        }
    }

    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var webCentralMessage by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(shift?.id, refreshToken) { summary = withContext(Dispatchers.IO) { repository.dailySummary() } }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val landscape = maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            ManagerHeader(shift, manager, onReturnToSale) { webCentralMessage = "La URL de Web Central se configurará con el entorno de la sede; las operaciones locales siguen disponibles." }
            webCentralMessage?.let { Text(it, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (landscape) Row(Modifier.weight(1f).fillMaxWidth()) {
                ManagerSideNav(section, { section = it }, availableSections, Modifier.width(188.dp).fillMaxHeight())
                HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                ManagerSectionContent(section, shift, manager, users, products, pendingEvents, summary, repository, { refreshToken++ }, onShiftClosed, Modifier.weight(1f))
            } else {
                ManagerCompactNav(section, { section = it }, availableSections)
                ManagerSectionContent(section, shift, manager, users, products, pendingEvents, summary, repository, { refreshToken++ }, onShiftClosed, Modifier.weight(1f))
            }
        }
    }
}

// Keeps return and Web Central shortcuts visible in every section.
@Composable
private fun ManagerHeader(shift: ShiftEntity?, manager: LocalUserEntity, onReturn: () -> Unit, onOpenWebCentral: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosButton(if (shift != null) "Volver a caja" else "Volver", onReturn)
        Column(Modifier.weight(1f)) {
            Text("Panel de control", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (shift != null) "Turno ${shift.id.take(4).uppercase()} · ${manager.displayName}"
                else "Sin turno activo · ${manager.displayName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PosButton("Web Central", onOpenWebCentral)
    }
}

// Provides persistent landscape navigation.
@Composable
private fun ManagerSideNav(selected: ManagerSection, choose: (ManagerSection) -> Unit, sections: List<ManagerSection>, modifier: Modifier) { Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { sections.forEach { item -> PosButton(item.label, { choose(item) }, selected = selected == item, modifier = Modifier.fillMaxWidth()) } } }

// Uses a compact selector in portrait.
@Composable
private fun ManagerCompactNav(selected: ManagerSection, choose: (ManagerSection) -> Unit, sections: List<ManagerSection>) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        PosButton("Sección: ${selected.label}", { expanded = true }, modifier = Modifier.fillMaxWidth())
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { sections.forEach { item -> DropdownMenuItem(text = { Text(item.label) }, onClick = { choose(item); expanded = false }) } }
    }
}

// Routes each Manager area while preserving the local session.
@Composable
private fun ManagerSectionContent(section: ManagerSection, shift: ShiftEntity?, manager: LocalUserEntity, users: List<LocalUserEntity>, products: List<ProductEntity>, pendingEvents: Int, summary: DashboardSummary?, repository: PosRepository, refresh: () -> Unit, onShiftClosed: () -> Unit, modifier: Modifier) {
    when (section) {
        ManagerSection.DASHBOARD -> DashboardPanel(summary, pendingEvents, modifier)
        ManagerSection.Z_CLOSE -> if (shift != null) ZClosePanel(shift, manager, users, summary, repository, refresh, onShiftClosed, modifier)
        ManagerSection.HISTORY -> if (shift != null) HistoryPanel(shift, manager, repository, refresh, modifier)
        ManagerSection.PRODUCTS -> ProductsPanel(products, repository, modifier)
        ManagerSection.STATUS -> StatusPanel(pendingEvents, products, repository, modifier)
    }
}

// Lists the local catalog so a manager can find a product and open its editor.
@Composable
private fun ProductsPanel(products: List<ProductEntity>, repository: PosRepository, modifier: Modifier) {
    var category by rememberSaveable { mutableStateOf("Todos") }
    var search by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sandbox = repository.mode() != RuntimeMode.PRODUCTION
    val categories = remember(products) { listOf("Todos") + products.map { it.saleCategory }.filter { it.isNotBlank() }.distinct() }
    val filtered = remember(products, category, search) {
        products.filter { product ->
            val matchesCategory = category == "Todos" || product.saleCategory.equals(category, ignoreCase = true)
            val query = search.trim()
            val matchesSearch = query.isEmpty() ||
                product.name.contains(query, ignoreCase = true) ||
                product.sku.contains(query, ignoreCase = true) ||
                product.barcode?.contains(query, ignoreCase = true) == true
            matchesCategory && matchesSearch
        }
    }
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Productos", style = MaterialTheme.typography.headlineSmall)
            Text("Busca por nombre, SKU o código. Editar no quita el producto de la sede.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar producto") },
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { item ->
                    PosButton(item, { category = item }, selected = category == item)
                }
            }
            if (filtered.isEmpty()) {
                Text("No hay productos con ese filtro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.id }) { product ->
                        ProductEditRow(product) {
                            error = null
                            editing = product
                        }
                    }
                }
            }
        }
    }
    editing?.let { product ->
        EditPosProductDialog(
            productName = product.name,
            sku = product.sku,
            category = product.saleCategory,
            initialBarcode = scanCode(product.barcode, product.sku),
            initialPriceCentavos = Money.fromCatalog(product.price),
            initialControlled = product.stockPolicy == "CONTROLLED",
            sandbox = sandbox,
            busy = busy,
            error = error,
            onDismiss = { if (!busy) editing = null },
            onSubmit = { name, cents, controlled, barcode ->
                val plan = planProductEdit(
                    product.name,
                    Money.fromCatalog(product.price),
                    product.stockPolicy == "CONTROLLED",
                    product.barcode,
                    product.sku,
                    name,
                    cents,
                    controlled,
                    barcode,
                )
                if (!plan.rename && !plan.offer) {
                    editing = null
                    return@EditPosProductDialog
                }
                busy = true
                error = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        if (sandbox) {
                            repository.updateTrainingProduct(product, name, cents, controlled, barcode)
                        } else {
                            val app = context.applicationContext as PosApplication
                            ProvisioningRepository(context, app.databaseProvider).updateProduct(product, name, cents, controlled, barcode)
                        }
                    }
                    busy = false
                    result.fold(
                        onSuccess = { editing = null },
                        onFailure = { error = it.message ?: "No se pudo guardar el producto." },
                    )
                }
            },
        )
    }
}

// One catalog row: name, price, stock flag, and the edit action.
@Composable
private fun ProductEditRow(product: ProductEntity, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(product.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(
                buildString {
                    append(product.saleCategory.ifBlank { "Sin categoría" })
                    append(" · ")
                    append(if (product.stockPolicy == "CONTROLLED") "Con inventario" else "Sin inventario")
                    product.sku.takeIf { it.isNotBlank() }?.let { append(" · "); append(it) }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(Money.format(Money.fromCatalog(product.price)), fontWeight = FontWeight.SemiBold)
        PosButton("Editar", onEdit)
    }
}

// Shows daily local metrics as flat operational tiles.
@Composable
private fun DashboardPanel(summary: DashboardSummary?, pendingEvents: Int, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Resumen del día", style = MaterialTheme.typography.headlineSmall)
            Text("Métricas acumuladas de toda la jornada (suma de todos los turnos del día en esta tablet).", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (summary == null) Text("Cargando resumen local…", color = MaterialTheme.colorScheme.onSurfaceVariant) else {
                MetricGrid(summary, pendingEvents)
                Text("Productos más vendidos hoy", style = MaterialTheme.typography.titleMedium)
                if (summary.topProducts.isEmpty()) EmptySurface("Aún no hay ventas registradas hoy.") else summary.topProducts.forEachIndexed { index, product ->
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("${index + 1}", modifier = Modifier.width(32.dp)); Text(product.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${product.quantity} · ${Money.format(product.amountCentavos)}", fontWeight = FontWeight.SemiBold) } }
                }
            }
        }
    }
}

// Renders the dashboard metric grid responsively.
@Composable
private fun MetricGrid(summary: DashboardSummary, pendingEvents: Int) {
    MetricTiles(
        listOf(
            "Ventas netas" to Money.format(summary.netCentavos),
            "Ventas brutas" to Money.format(summary.grossCentavos),
            "Descuentos / cortesías" to Money.format(summary.discountsCentavos),
            "Tickets" to summary.ticketCount.toString(),
            "Ticket promedio" to Money.format(summary.averageTicketCentavos),
            "Efectivo cobrado" to Money.format(summary.cashCollectedCentavos),
            "Sangrías" to "${summary.withdrawalCount} · ${Money.format(summary.withdrawalsCentavos)}",
            "Mermas" to summary.wasteCount.toString(),
            "Ventas canceladas" to summary.cancelledCount.toString(),
            "Pendientes sync" to pendingEvents.toString(),
        )
    )
}

// Lays out compact supervision tiles without exposing an accounting ledger.
@Composable
private fun MetricTiles(metrics: List<Pair<String, String>>) {
    BoxWithConstraints {
        val columns = if (maxWidth >= 540.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            metrics.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, value) -> MetricTile(label, value, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// Creates a flat metric tile with a clear numeric hierarchy.
@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) { Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) } } }

// Handles the operational cash summary, blind count, rejection, and final approval.
@Composable
private fun ZClosePanel(
    shift: ShiftEntity,
    manager: LocalUserEntity,
    users: List<LocalUserEntity>,
    summary: DashboardSummary?,
    repository: PosRepository,
    refresh: () -> Unit,
    onShiftClosed: () -> Unit,
    modifier: Modifier
) {
    var zCloseOpen by remember(shift.id) { mutableStateOf(false) }
    var withdrawalsOpen by remember(shift.id) { mutableStateOf(false) }
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
    var stage by remember(shift.id) { mutableStateOf(CountStage.COUNTING) }
    var count by remember(shift.id) { mutableStateOf("") }
    var attempt by remember(shift.id) { mutableStateOf<CashCountAttemptEntity?>(null) }
    var rejectionReason by remember(shift.id) { mutableStateOf("") }
    var message by remember(shift.id) { mutableStateOf<String?>(null) }
    var pinRequested by remember(shift.id) { mutableStateOf(false) }
    var printSummaryTicket by remember { mutableStateOf(true) }
    var liveClose by remember(shift.id) { mutableStateOf(close) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(shift.id, stage) {
        liveClose = withContext(Dispatchers.IO) { repository.shiftCloseBreakdown(shift) }
    }

    fun submit() {
        val amount = Money.fromInput(count)
        if (amount == null || amount < 0) {
            message = "Captura un conteo válido."
        } else scope.launch {
            attempt = withContext(Dispatchers.IO) { repository.submitCashCount(shift, amount, "total=$amount") }
            if (attempt == null) {
                message = "No se pudo guardar el conteo local."
            } else {
                SyncWorker.enqueue(context)
                stage = CountStage.VALIDATION
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
                            PosButton("Enviar a validación", ::submit, primary = true, modifier = Modifier.weight(1f))
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
                            label = { Text("Motivo si se devuelve a corrección") },
                            modifier = Modifier.fillMaxWidth()
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
                            "Haz clic en el botón a continuación para concluir y salir a la pantalla de apertura de turno.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        PosButton(
                            label = "Finalizar y salir al inicio →",
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
            scope.launch {
                val result = withContext(Dispatchers.IO) { repository.approveShiftClose(shift, currentAttempt, signingManager, pin, printSummaryTicket) }
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

// Shows immutable safeguard withdrawals in a compact read-only list.
@Composable
private fun WithdrawalsPanel(shift: ShiftEntity, repository: PosRepository, modifier: Modifier) {
    var withdrawals by remember(shift.id) { mutableStateOf<List<CashWithdrawalEntity>>(emptyList()) }
    LaunchedEffect(shift.id) { withdrawals = withContext(Dispatchers.IO) { repository.withdrawals(shift.id) } }
    val total = withdrawals.sumOf { it.amountCentavos }
    Surface(modifier, color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Sangrías de resguardo", style = MaterialTheme.typography.headlineSmall); Text("Consulta de solo lectura · se registran desde la barra de caja.", color = MaterialTheme.colorScheme.onSurfaceVariant); MetricTile("Total retirado · ${withdrawals.size} registros", Money.format(total)); if (withdrawals.isEmpty()) EmptySurface("No hay sangrías en este turno.") else withdrawals.forEach { withdrawal -> Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("${withdrawal.folio} · ${Money.format(withdrawal.amountCentavos)}", style = MaterialTheme.typography.titleMedium); Text("Cajero ${withdrawal.cashierId} · Autorizó ${withdrawal.authorizedByUserId}", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Comprobante pendiente de hardware", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } }
}

// Displays real tickets from the current shift and queues reprints locally.
@Composable
private fun HistoryPanel(shift: ShiftEntity, manager: LocalUserEntity, repository: PosRepository, refresh: () -> Unit, modifier: Modifier) {
    var sales by remember(shift.id) { mutableStateOf<List<SaleEntity>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var pageSize by rememberSaveable(query) { mutableIntStateOf(10) }
    var message by remember { mutableStateOf<String?>(null) }
    var cancelSale by remember { mutableStateOf<SaleEntity?>(null) }
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
                label = { Text("Buscar por folio (búsqueda parcial)") },
                placeholder = { Text("Ej. 0001") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (filtered.isEmpty()) {
                EmptySurface("No se encontraron tickets en este turno para la búsqueda.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(visibleSales, key = { it.id }) { sale ->
                        SaleHistoryRow(sale, repository, { message = it; refresh() }, { cancelSale = sale })
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
            onDismiss = { cancelSale = null },
            onDone = { notice -> message = notice; cancelSale = null; scope.launch { sales = withContext(Dispatchers.IO) { repository.salesForShift(shift.id) }; refresh() } },
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
    var reason by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
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
                OutlinedTextField(reason, { reason = it }, label = { Text("Motivo obligatorio") }, modifier = Modifier.fillMaxWidth())
                Text("PIN de ${manager.displayName}", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cerrar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Confirmar cancelación", {
                        if (reason.isBlank() || pin.length < 4) {
                            message = "Captura motivo y PIN de cuatro dígitos."
                        } else scope.launch {
                            val validPin = withContext(Dispatchers.IO) { repository.authenticate(manager.id, pin) }
                            if (!validPin) {
                                message = "PIN de ${manager.displayName} incorrecto."
                                pin = ""
                                return@launch
                            }
                            val ok = withContext(Dispatchers.IO) { repository.cancelCashSale(sale, manager, pin, reason) }
                            if (ok) {
                                SyncWorker.enqueue(context)
                                onDone("Venta ${sale.folio} cancelada y auditada.")
                            } else message = "No se pudo cancelar la venta."
                        }
                    }, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Presents durable queue state and the currently available peripheral runtime.
@Composable
private fun StatusPanel(
    pendingEvents: Int,
    products: List<ProductEntity>,
    repository: PosRepository,
    modifier: Modifier,
) {
    val printJobs = remember { mutableStateOf(emptyList<PrintJobEntity>()) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var peripheralMessage by remember { mutableStateOf<String?>(null) }
    var device by remember { mutableStateOf<DeviceEntity?>(null) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var updateBusy by remember { mutableStateOf(false) }
    var availableUpdateName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mode = repository.mode()
    val updater = remember(context) { PosAppUpdater(context) }
    var printerAvailability by remember(mode) { mutableStateOf(PrinterFactory.availability(context, mode)) }
    var savedMac by remember { mutableStateOf(PrinterPreferences(context).mac()) }
    var bonded by remember { mutableStateOf(emptyList<BondedPrinter>()) }
    var bluetoothGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        bluetoothGranted = granted
        if (granted) bonded = bondedPrinters(context)
        printerAvailability = PrinterFactory.availability(context, mode)
    }

    fun refreshPrinter() {
        printerAvailability = PrinterFactory.availability(context, mode)
        savedMac = PrinterPreferences(context).mac()
        if (bluetoothGranted) bonded = bondedPrinters(context)
    }

    LaunchedEffect(mode, bluetoothGranted) {
        refreshPrinter()
        PosPrinterRegistry.statusTick.collect { refreshPrinter() }
    }
    LaunchedEffect(mode, bluetoothGranted) {
        while (true) {
            delay(2_000)
            refreshPrinter()
        }
    }

    fun refreshPrintJobs() {
        scope.launch {
            printJobs.value = withContext(Dispatchers.IO) { repository.pendingPrintJobs() }
        }
    }

    fun applyCheckOutcome(outcome: ReleaseCheckOutcome, announceUpToDate: Boolean) {
        when (outcome) {
            is ReleaseCheckOutcome.UpdateAvailable -> {
                availableUpdateName = outcome.remote.versionName
                updateMessage = "Hay una versión nueva: v${outcome.remote.versionName}."
            }
            is ReleaseCheckOutcome.UpToDate -> {
                availableUpdateName = null
                if (announceUpToDate) {
                    val remoteName = outcome.remote.versionName.ifBlank { BuildConfig.VERSION_NAME }
                    updateMessage = "Estás al día (v$remoteName)."
                }
            }
            is ReleaseCheckOutcome.Failed -> {
                updateMessage = outcome.message
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPrintJobs()
        device = withContext(Dispatchers.IO) { repository.device() }
        availableUpdateName = updater.cachedUpdateVersionName()
        updateBusy = true
        val outcome = withContext(Dispatchers.IO) { updater.check(force = false) }
        updateBusy = false
        applyCheckOutcome(outcome, announceUpToDate = false)
    }

    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Estado y periféricos", style = MaterialTheme.typography.headlineSmall)
            MetricTile("Eventos pendientes", pendingEvents.toString())
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aplicación", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Versión ${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (mode == RuntimeMode.SANDBOX) "Modo Capacitación" else "Modo Venta",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val currentDevice = device
                    if (currentDevice != null) {
                        Text(
                            "Dispositivo ${currentDevice.name} · ${currentDevice.visibleCode}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Estado ${currentDevice.status}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Mínima requerida ${currentDevice.minAppVersion ?: "—"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Dispositivo aún no enrolado en este modo.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PosButton(
                        if (updateBusy) "Consultando…" else "Buscar actualización",
                        {
                            scope.launch {
                                updateBusy = true
                                updateMessage = null
                                val outcome = withContext(Dispatchers.IO) { updater.check(force = true) }
                                updateBusy = false
                                applyCheckOutcome(outcome, announceUpToDate = true)
                            }
                        },
                        enabled = !updateBusy,
                    )
                    val pendingName = availableUpdateName
                    if (pendingName != null) {
                        PosButton(
                            if (updateBusy) "Descargando…" else "Actualizar a v$pendingName",
                            {
                                scope.launch {
                                    updateBusy = true
                                    updateMessage = "Descargando APK…"
                                    when (val result = withContext(Dispatchers.IO) { updater.downloadAndInstall() }) {
                                        ApkInstallOutcome.Started ->
                                            updateMessage =
                                                "Instalador abierto. Confirma la actualización sin desinstalar la app."
                                        ApkInstallOutcome.NeedsInstallPermission -> {
                                            updateMessage =
                                                "Activa «Instalar apps desconocidas» para Pimienta POS y vuelve a intentar."
                                            runCatching {
                                                context.startActivity(updater.installPermissionSettingsIntent())
                                            }
                                        }
                                        is ApkInstallOutcome.Failed -> updateMessage = result.message
                                    }
                                    updateBusy = false
                                }
                            },
                            enabled = !updateBusy,
                            primary = true,
                        )
                    }
                    updateMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sincronización", style = MaterialTheme.typography.titleMedium)
                    Text("Offline-first · los eventos permanecen en Room hasta sincronizar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PosButton("Intentar sincronizar ahora", {
                        if (mode == RuntimeMode.PRODUCTION) {
                            syncMessage = "Sincronizando…"
                            scope.launch {
                                syncMessage = withContext(Dispatchers.IO) { runForegroundSync(context) }
                            }
                        } else {
                            syncMessage = "Sandbox no envía eventos al backend."
                        }
                    })
                    syncMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Impresión", style = MaterialTheme.typography.titleMedium)
                    val failed = printJobs.value.count { it.status == "FAILED" }
                    Text("${printJobs.value.size} trabajos pendientes · $failed fallidos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        printerStatusPresentation(mode, printerAvailability.status, printerAvailability.link).label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Si la tablet está cargando, elige la térmica ya emparejada en Ajustes.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!bluetoothGranted) {
                        PosButton("Permitir Bluetooth", { bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) })
                    } else if (bonded.isEmpty()) {
                        Text(
                            "No hay equipos Bluetooth emparejados.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        bonded.forEach { printer ->
                            val selected = printer.mac.equals(savedMac, ignoreCase = true)
                            PosButton(
                                if (selected) "${printer.name} · en uso" else printer.name,
                                {
                                    PrinterPreferences(context).saveMac(printer.mac)
                                    refreshPrinter()
                                    PosPrinterRegistry.notifyChanged()
                                    peripheralMessage = "Impresora Bluetooth: ${printer.name}."
                                },
                                primary = selected,
                            )
                        }
                    }
                    if (savedMac != null) {
                        PosButton("Olvidar impresora Bluetooth", {
                            PrinterPreferences(context).clearMac()
                            refreshPrinter()
                            PosPrinterRegistry.notifyChanged()
                            peripheralMessage = "Impresora Bluetooth olvidada."
                        })
                    }
                    PosButton("Procesar cola de impresión", {
                        PrintWorker.enqueue(context)
                        refreshPrintJobs()
                        peripheralMessage = "Cola de impresión encolada."
                    })
                    PosButton("Imprimir y abrir cajón", {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                val printer = PrinterFactory.create(context, mode)
                                val encoder = EscPosEncoder(printer.profile)
                                val document = OperationalDocument(
                                    title = "Prueba de impresión",
                                    folio = "TEST",
                                    occurredAt = Instant.now(),
                                    lines = listOf(PrintableLine("POS-5890A", "1", 0)),
                                    totalCentavos = 0,
                                )
                                printer.print(encoder.encode(document, openDrawer = true))
                            }
                            val link = PrinterFactory.availability(context, mode)
                            peripheralMessage = when {
                                result is PrintResult.Printed && link.link == PrinterLink.USB &&
                                    link.status == PeripheralStatus.READY ->
                                    "Prueba enviada por USB."
                                result is PrintResult.Printed && link.link == PrinterLink.BLUETOOTH &&
                                    link.status == PeripheralStatus.READY ->
                                    "Prueba enviada por Bluetooth."
                                result is PrintResult.Printed -> "Prueba simulada. No hay impresora USB ni Bluetooth."
                                result is PrintResult.Failed -> "Impresión fallida: ${result.reason.name}"
                                else -> "Impresión fallida."
                            }
                        }
                    })
                    peripheralMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lector", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            PosScannerRegistry.fake != null -> "Fake scanner + lector en modo teclado activos"
                            else -> "Lector en modo teclado pendiente"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Con la tablet cargando, el lector Bluetooth en modo teclado se usa igual que el USB.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PosButton("Probar lectura conocida", {
                        val code = products.firstNotNullOfOrNull { it.barcode?.takeIf(String::isNotBlank) }
                            ?: products.firstOrNull()?.sku
                            ?: "7501234567890"
                        PosScannerRegistry.fake?.emit(code)
                        peripheralMessage = "Lectura fake enviada: $code"
                    })
                    PosButton("Probar código desconocido", {
                        PosScannerRegistry.fake?.emit("9999999999999")
                        peripheralMessage = "Lectura fake desconocida enviada."
                    })
                }
            }
        }
    }
}

// Confirms a Manager PIN for high-impact actions such as sealing a shift.
@Composable
internal fun ManagerPinDialog(
    users: List<LocalUserEntity>,
    title: String,
    initialManager: LocalUserEntity? = null,
    repository: PosRepository? = null,
    onDismiss: () -> Unit,
    onApproved: (LocalUserEntity, String) -> Unit
) {
    val isTraining = repository?.mode() == RuntimeMode.SANDBOX
    val managers = remember(users) {
        users.filter { it.active && it.isManagerOrAdmin }
    }
    var selected by remember { mutableStateOf(initialManager ?: managers.firstOrNull()) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        val authorizer = selected
        if (authorizer == null) {
            error = "Selecciona un perfil de Gerente o Administrador."
            return
        }
        if (pin.length < 4) {
            error = "Captura los cuatro dígitos del PIN."
            return
        }
        if (repository != null) {
            scope.launch {
                busy = true
                val valid = withContext(Dispatchers.IO) { repository.authenticate(authorizer.id, pin) }
                busy = false
                if (valid) {
                    onApproved(authorizer, pin)
                } else {
                    error = "PIN de ${authorizer.displayName} incorrecto."
                    pin = ""
                }
            }
        } else {
            onApproved(authorizer, pin)
        }
    }

    Dialog(onDismissRequest = { /* Prevent accidental dismiss on backdrop tap for security */ }) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text("Selecciona el autorizador (Gerente o Administrador):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (managers.isEmpty()) {
                    Text("No hay usuarios Gerente o Administrador activos.", color = MaterialTheme.colorScheme.error)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        managers.forEach { user ->
                            PosButton(
                                label = user.displayTitle(isTraining),
                                click = { selected = user; error = null },
                                selected = selected?.id == user.id,
                            )
                        }
                    }
                }
                Text("PIN de ${selected?.displayName ?: "Gerente o Administrador"}", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, enabled = !busy, modifier = Modifier.weight(1f))
                    PosButton(
                        label = if (busy) "Verificando…" else if (title.contains("Autorizar", ignoreCase = true)) "Autorizar" else "Firmar",
                        click = ::submit,
                        enabled = !busy && pin.length >= 4 && selected != null,
                        primary = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// Keeps empty states explicit instead of rendering a blank surface.
@Composable
private fun EmptySurface(message: String) { Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Text(message, Modifier.fillMaxWidth().padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }

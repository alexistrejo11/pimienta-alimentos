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

// Describes the local Manager workspace navigation.
private enum class ManagerSection(val label: String) {
    DASHBOARD("Resumen del día"),
    Z_CLOSE("Caja y Corte de Caja"),
    HISTORY("Historial"),
    PRODUCTS("Productos"),
    CONFIG("Configuración"),
    STATUS("Estado"),
}
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
    var activeShift by remember { mutableStateOf(shift) }
    LaunchedEffect(shift) {
        if (shift != null) activeShift = shift
    }
    val effectiveShift = activeShift ?: shift

    val availableSections = remember(effectiveShift) {
        if (effectiveShift != null) {
            ManagerSection.entries
        } else {
            listOf(ManagerSection.DASHBOARD, ManagerSection.PRODUCTS, ManagerSection.CONFIG, ManagerSection.STATUS)
        }
    }
    var section by rememberSaveable { mutableStateOf(ManagerSection.DASHBOARD) }

    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var webCentralMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(effectiveShift?.id, refreshToken) { summary = withContext(Dispatchers.IO) { repository.dailySummary() } }

    val sectionContent = remember {
        movableContentOf { modifier: Modifier ->
            ManagerSectionContent(
                section = section,
                shift = effectiveShift,
                manager = manager,
                users = users,
                products = products,
                pendingEvents = pendingEvents,
                summary = summary,
                repository = repository,
                refresh = { refreshToken++ },
                onShiftClosed = onShiftClosed,
                modifier = modifier,
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val landscape = maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            ManagerHeader(effectiveShift, manager, onReturnToSale) { webCentralMessage = "La URL de Web Central se configurará con el entorno de la sede; las operaciones locales siguen disponibles." }
            webCentralMessage?.let { Text(it, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (landscape) Row(Modifier.weight(1f).fillMaxWidth()) {
                ManagerSideNav(section, { section = it }, availableSections, Modifier.width(188.dp).fillMaxHeight())
                HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                sectionContent(Modifier.weight(1f))
            } else {
                ManagerCompactNav(section, { section = it }, availableSections)
                sectionContent(Modifier.weight(1f))
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosButton(
            label = if (shift != null) "Volver a caja" else "Volver",
            click = onReturn,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        )
        Column(Modifier.weight(1f)) {
            Text("Panel de control", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (shift != null) "Turno ${shift.id.take(4).uppercase()} · ${manager.displayName}"
                else "Sin turno activo · ${manager.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PosButton(
            label = "Web Central",
            click = onOpenWebCentral,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// Provides persistent landscape navigation.
@Composable
private fun ManagerSideNav(selected: ManagerSection, choose: (ManagerSection) -> Unit, sections: List<ManagerSection>, modifier: Modifier) {
    Column(modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sections.forEach { item ->
            PosButton(
                label = item.label,
                click = { choose(item) },
                selected = selected == item,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

// Uses a compact selector in portrait.
@Composable
private fun ManagerCompactNav(selected: ManagerSection, choose: (ManagerSection) -> Unit, sections: List<ManagerSection>) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        PosButton("Sección: ${selected.label}", { expanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp))
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
        ManagerSection.CONFIG -> ConfigPanel(repository, modifier)
        ManagerSection.STATUS -> StatusPanel(pendingEvents, repository, modifier)
    }
}

// Local tablet toggles that do not come from server policy sync.
@Composable
private fun ConfigPanel(repository: PosRepository, modifier: Modifier) {
    var kitchenTicketFilter by remember { mutableStateOf(repository.kitchenTicketPrintFilterEnabled()) }

    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Configuración local", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Estos ajustes aplican solo a esta tablet. No se sincronizan con la web central.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Impresión de tickets", style = MaterialTheme.typography.titleMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                kitchenTicketFilter = !kitchenTicketFilter
                                repository.setKitchenTicketPrintFilterEnabled(kitchenTicketFilter)
                            },
                    ) {
                        Checkbox(
                            checked = kitchenTicketFilter,
                            onCheckedChange = { checked ->
                                kitchenTicketFilter = checked
                                repository.setKitchenTicketPrintFilterEnabled(checked)
                            },
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                "Imprimir ticket solo para preparación y productos sin código de proveedor",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Chilaquiles y platillos sin barcode de empaque sí imprimen (cocina y cliente). " +
                                    "Un carrito solo de empaquetados con barcode (ej. Boing) no imprime ticket, " +
                                    "pero en efectivo sí se abre el cajón. Si mezclas empaque y preparación, sí imprime. " +
                                    "Desactiva para imprimir todas las ventas.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "La reimpresión desde Historial sigue disponible cuando haga falta un ticket.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// Lists the local catalog so a manager can find a product and open its editor.
@Composable
private fun ProductsPanel(products: List<ProductEntity>, repository: PosRepository, modifier: Modifier) {
    var category by rememberSaveable { mutableStateOf("Todos") }
    var search by rememberSaveable { mutableStateOf("") }
    var editingProductId by rememberSaveable { mutableStateOf<String?>(null) }

    // Observes live catalog changes from Room so updates and creations reflect instantly without reloading the screen.
    val liveProducts by remember(repository, products) {
        repository.observeProducts()
    }.collectAsState(initial = products)

    // Listens to hardware barcode scanner reads to populate the search field automatically.
    val scanner = remember { PosScannerRegistry.primary }
    LaunchedEffect(scanner) {
        scanner ?: return@LaunchedEffect
        scanner.start()
        scanner.events.collect { read ->
            val code = read.rawValue.trim()
            if (code.isNotBlank()) {
                search = code
            }
        }
    }

    val editing = remember(editingProductId, liveProducts) { liveProducts.firstOrNull { it.id == editingProductId } }
    var creatingProduct by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var createBusy by rememberSaveable { mutableStateOf(false) }
    var syncBusy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var createError by rememberSaveable { mutableStateOf<String?>(null) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sandbox = repository.mode() != RuntimeMode.PRODUCTION
    val (searchInteraction, forceSearchKeyboard) = rememberForceSoftKeyboardInteractionSource()
    val categories = remember(liveProducts) { listOf("Todos") + sortedCategoryNames(liveProducts.map { it.saleCategory }) }
    val filtered = remember(liveProducts, category, search) {
        liveProducts.filter { product ->
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
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Productos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "(${filtered.size}/${liveProducts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PosButton(
                        label = if (syncBusy) "Sincronizando…" else "Sincronizar",
                        click = {
                            if (sandbox) {
                                syncMessage = "Capacitación: catálogo local."
                            } else {
                                syncBusy = true
                                syncMessage = "Sincronizando con servidor…"
                                scope.launch {
                                    val msg = withContext(Dispatchers.IO) { runForegroundSync(context) }
                                    syncBusy = false
                                    syncMessage = if (msg == "Sincronización completada.") "✓ Sincronización completada." else msg
                                }
                            }
                        },
                        enabled = !syncBusy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    )
                    PosButton(
                        label = "Agregar producto",
                        click = {
                            createError = null
                            creatingProduct = true
                        },
                        primary = true,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            syncMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) forceSearchKeyboard() },
                interactionSource = searchInteraction,
                singleLine = true,
                placeholder = { Text("Buscar por nombre o código…", style = MaterialTheme.typography.bodySmall) },
                colors = catalogFieldColors(),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { item ->
                    PosButton(
                        label = item,
                        click = { category = item },
                        selected = category == item,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            if (filtered.isEmpty()) {
                Text("No hay productos con ese filtro.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(filtered, key = { it.id }) { product ->
                        ProductEditRow(product) {
                            error = null
                            editingProductId = product.id
                        }
                    }
                }
            }
        }
    }
    if (creatingProduct) {
        val createCategories = remember(liveProducts) {
            val distinct = sortedCategoryNames(liveProducts.map { it.saleCategory })
            if (distinct.isEmpty()) listOf("General") else distinct
        }
        CreatePosProductDialog(
            categories = createCategories,
            sandbox = sandbox,
            busy = createBusy,
            error = createError,
            onDismiss = {
                if (!createBusy) {
                    creatingProduct = false
                    createError = null
                }
            },
            onSubmit = { name, cat, cents, barcode, controlled ->
                createBusy = true
                createError = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        if (sandbox) {
                            repository.createTrainingProduct(name, cents, cat, barcode, controlled)
                        } else {
                            val app = context.applicationContext as PosApplication
                            ProvisioningRepository(context, app.databaseProvider).createProduct(
                                name = name,
                                salePriceCentavos = cents,
                                saleCategory = cat,
                                barcode = barcode,
                                createdByOperatorId = null,
                                controlledStock = controlled,
                            )
                        }
                    }
                    createBusy = false
                    result.fold(
                        onSuccess = {
                            creatingProduct = false
                        },
                        onFailure = {
                            createError = if (sandbox) {
                                it.message ?: "No se pudo guardar el producto."
                            } else {
                                PosApiUserMessages.from(it)
                            }
                        },
                    )
                }
            },
        )
    }
    editing?.let { product ->
        // Include the stored name when it is missing from the other products' categories.
        val editCategories = remember(liveProducts, product.saleCategory) {
            val distinct = sortedCategoryNames(liveProducts.map { it.saleCategory })
            val base = if (distinct.isEmpty()) listOf("General") else distinct
            if (base.none { it.equals(product.saleCategory, ignoreCase = true) } && product.saleCategory.isNotBlank()) {
                listOf(product.saleCategory) + base
            } else {
                base
            }
        }
        EditPosProductDialog(
            productName = product.name,
            categories = editCategories,
            category = product.saleCategory,
            initialBarcode = product.barcode?.trim().orEmpty(),
            initialPriceCentavos = Money.fromCatalog(product.price),
            initialControlled = product.stockPolicy == "CONTROLLED",
            sandbox = sandbox,
            busy = busy,
            error = error,
            onDismiss = { if (!busy) editingProductId = null },
            onSubmit = { name, category, cents, controlled, barcode ->
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
                    product.saleCategory,
                    category,
                )
                if (!plan.rename && !plan.offer) {
                    editingProductId = null
                    return@EditPosProductDialog
                }
                busy = true
                error = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        if (sandbox) {
                            repository.updateTrainingProduct(product, name, cents, controlled, barcode, category)
                        } else {
                            val app = context.applicationContext as PosApplication
                            ProvisioningRepository(context, app.databaseProvider).updateProduct(product, name, cents, controlled, barcode, category)
                        }
                    }
                    busy = false
                    result.fold(
                        onSuccess = { editingProductId = null },
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
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    product.barcode?.trim()?.takeIf { it.isNotEmpty() }?.let { append(" · "); append(it) }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(Money.format(Money.fromCatalog(product.price)), fontWeight = FontWeight.SemiBold)
        PosButton("Editar", onEdit, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp))
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
                            colors = catalogFieldColors(),
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

// App updates, sync, and the shared printer/scanner config used by cashiers.
@Composable
private fun StatusPanel(
    pendingEvents: Int,
    repository: PosRepository,
    modifier: Modifier,
) {
    var syncMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var device by remember { mutableStateOf<DeviceEntity?>(null) }
    var updateMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var updateBusy by rememberSaveable { mutableStateOf(false) }
    var availableUpdateName by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mode = repository.mode()
    val updater = remember(context) { PosAppUpdater(context) }

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
                    PosButton(if (syncing) "Sincronizando…" else "Intentar sincronizar ahora", {
                        if (syncing) return@PosButton
                        if (mode == RuntimeMode.PRODUCTION) {
                            syncMessage = "Sincronizando…"
                            syncing = true
                            scope.launch {
                                val msg = withContext(Dispatchers.IO) { runForegroundSync(context) }
                                syncing = false
                                syncMessage = if (msg == "Sincronización completada.") "✓ Sincronización completada." else msg
                            }
                        } else {
                            syncMessage = "Sandbox no envía eventos al backend."
                        }
                    }, enabled = !syncing)
                    syncMessage?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            DevicesPanel(repository = repository, modifier = Modifier.fillMaxWidth(), showHeading = true)
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
    var selectedId by rememberSaveable { mutableStateOf((initialManager ?: managers.firstOrNull())?.id) }
    val selected = remember(selectedId, managers) { managers.firstOrNull { it.id == selectedId } ?: initialManager ?: managers.firstOrNull() }
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by rememberSaveable { mutableStateOf(false) }
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
                                click = { selectedId = user.id; error = null },
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

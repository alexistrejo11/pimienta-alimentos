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
// Lists the local catalog so a manager can find a product and open its editor.
@Composable
internal fun ProductsPanel(products: List<ProductEntity>, repository: PosRepository, modifier: Modifier) {
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


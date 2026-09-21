package io.github.alexistrejo.pimienta.pos

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.domain.*
import io.github.alexistrejo.pimienta.pos.hardware.PosPrinterRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PrinterFactory
import io.github.alexistrejo.pimienta.pos.hardware.printerStatusPresentation
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Polls USB printer health for the sale status bar and USB attach events.
@Composable
internal fun rememberLivePrinterStatus(context: Context, mode: RuntimeMode): Pair<String, Boolean> {
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(mode) {
        PosPrinterRegistry.statusTick.collect { refresh++ }
    }
    LaunchedEffect(mode) {
        while (true) {
            delay(2_000)
            refresh++
        }
    }
    val presentation = remember(refresh, mode) {
        printerStatusPresentation(mode, PrinterFactory.printerStatus(context, mode))
    }
    return presentation.label to presentation.alert
}

// Keeps operational status visible in a compact collapsible top bar.
@Composable
internal fun StatusBar(
    cashier: String,
    pending: Int,
    syncLabel: String,
    dark: Boolean,
    onTheme: (Boolean) -> Unit,
    landscape: Boolean,
    lockCashRegister: () -> Unit,
    openManager: () -> Unit,
    openWithdrawal: () -> Unit,
    withdrawalEnabled: Boolean,
    printerLabel: String,
    printerAlert: Boolean,
    scannerLabel: String,
    onCreateProduct: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    syncBusy: Boolean = false,
    createProductEnabled: Boolean = true,
) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val headerBtnPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { expanded = !expanded }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                if (landscape) "Punto de Venta" else "POS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(if (landscape) "Turno: $cashier" else cashier)
                StatusChip(printerLabel, alert = printerAlert)
                if (!expanded && (pending > 0 || syncLabel.contains("desactualizado", ignoreCase = true))) {
                    StatusChip(syncLabel, alert = syncLabel.contains("desactualizado", ignoreCase = true))
                }
            }
            PosButton(
                label = if (expanded) "Ocultar ▲" else "Acciones ▼",
                click = { expanded = !expanded },
                contentPadding = headerBtnPadding,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PosButton("Agregar producto", onCreateProduct, enabled = createProductEnabled, contentPadding = headerBtnPadding)
                    PosButton(if (syncBusy) "Sincronizando…" else "Sincronizar", onSyncNow, enabled = !syncBusy, contentPadding = headerBtnPadding)
                    PosButton("Sangría", openWithdrawal, enabled = withdrawalEnabled, contentPadding = headerBtnPadding)
                    PosButton("Bloquear caja", lockCashRegister, contentPadding = headerBtnPadding)
                    PosButton("Panel Manager", openManager, contentPadding = headerBtnPadding)
                    PosButton(if (dark) "Tema claro" else "Tema oscuro", { onTheme(!dark) }, contentPadding = headerBtnPadding)
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusChip(syncLabel, alert = syncLabel.contains("desactualizado", ignoreCase = true))
                    StatusChip("Tablet T1")
                    StatusChip(scannerLabel)
                }
            }
        }
    }
}

// Captures price for an unknown barcode without creating a catalog product by default.
@Composable
internal fun PendingCatalogDialog(
    barcode: String,
    openAmountAvailable: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onSaveToCatalog: () -> Unit = {},
    onOpenAmount: () -> Unit = {},
) {
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val cents = Money.fromInput(amount)
        if (cents == null || cents <= 0) {
            error = "Captura un importe válido."
            return
        }
        onConfirm(cents)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Código no está en el catálogo", style = MaterialTheme.typography.titleLarge)
                Text("Código escaneado", style = MaterialTheme.typography.labelLarge)
                Text(barcode, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Cobra esta venta con el importe, o guarda el producto si ya conoces el nombre.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Importe", style = MaterialTheme.typography.labelLarge)
                Text(Money.format(Money.fromInput(amount) ?: 0), style = MaterialTheme.typography.headlineSmall)
                Numpad(amount, { amount = it }, onSubmit = ::submit)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Agregar a esta venta", ::submit, primary = true, modifier = Modifier.weight(1f))
                }
                PosButton("Guardar en catálogo", onSaveToCatalog, modifier = Modifier.fillMaxWidth())
                if (openAmountAvailable) {
                    PosButton("Usar Monto abierto", onOpenAmount, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// Creates a POS-sellable product (server in production, local scratch in training).
@Composable
internal fun CreatePosProductDialog(
    categories: List<String>,
    lockedBarcode: String? = null,
    initialPriceCentavos: Long? = null,
    sandbox: Boolean,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, category: String, priceCentavos: Long, barcode: String?, controlled: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var amount by remember { mutableStateOf(initialPriceCentavos?.let { BigDecimal.valueOf(it, 2).toPlainString() } ?: "") }
    var barcode by remember { mutableStateOf(lockedBarcode.orEmpty()) }
    var controlled by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val cents = Money.fromInput(amount)
        when {
            name.isBlank() -> localError = "Captura el nombre."
            category.isBlank() -> localError = "Elige una categoría de venta."
            cents == null || cents <= 0 -> localError = "Captura un precio válido."
            else -> {
                localError = null
                onSubmit(name.trim(), category, cents, barcode.trim().ifBlank { null }, controlled)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Nuevo producto", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (sandbox) {
                        "Capacitación: se guarda en esta tablet y desaparece al salir o al reiniciar. No se envía al servidor."
                    } else {
                        "El SKU lo asigna el servidor. Sin código queda como producto interno."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true, colors = catalogFieldColors())
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        PosButton(cat, { category = cat }, selected = category == cat)
                    }
                }
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { if (lockedBarcode == null) barcode = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Código de barras (opcional)") },
                    singleLine = true,
                    readOnly = lockedBarcode != null,
                    colors = catalogFieldColors(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = controlled, onCheckedChange = { controlled = it })
                    Text("Controla inventario")
                }
                Text("Precio", style = MaterialTheme.typography.labelLarge)
                Text(Money.format(Money.fromInput(amount) ?: 0), style = MaterialTheme.typography.headlineSmall)
                Numpad(amount, { amount = it }, onSubmit = ::submit)
                (error ?: localError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f), enabled = !busy)
                    PosButton(if (busy) "Guardando…" else "Guardar", ::submit, primary = true, modifier = Modifier.weight(1f), enabled = !busy)
                }
            }
        }
    }
}

// Captures category and amount; Manager PIN is requested by the sale screen afterwards.
@Composable
internal fun OpenAmountDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (category: String, centavos: Long) -> Unit,
    initialCategory: String? = null,
) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory ?: categories.firstOrNull().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val cents = Money.fromInput(amount)
        when {
            selectedCategory.isBlank() -> error = "Selecciona una categoría."
            cents == null || cents <= 0 -> error = "Captura un importe válido."
            else -> onConfirm(selectedCategory.trim(), cents)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Monto abierto", style = MaterialTheme.typography.titleLarge)
                if (initialCategory == null) {
                    Text("Categoría permitida", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { category ->
                            PosButton(category, { selectedCategory = category }, selected = selectedCategory == category)
                        }
                    }
                }
                Text("Descripción generada", style = MaterialTheme.typography.labelLarge)
                Text("Producto abierto · ${selectedCategory.ifBlank { "categoría" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Importe", style = MaterialTheme.typography.labelLarge)
                Text(Money.format(Money.fromInput(amount) ?: 0), style = MaterialTheme.typography.headlineSmall)
                Numpad(amount, { amount = it }, onSubmit = ::submit)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Agregar al carrito", ::submit, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Shows categories grouped in columns of up to 6 items to jump quickly between sections.
@Composable
internal fun SectionsDialog(
    categories: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val chunks = remember(categories) { categories.chunked(6) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Secciones del catálogo", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunks.forEach { columnCategories ->
                        Column(
                            modifier = Modifier.width(180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            columnCategories.forEach { category ->
                                PosButton(
                                    label = category,
                                    click = {
                                        onSelect(category)
                                        onDismiss()
                                    },
                                    selected = selected == category,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                PosButton("Cerrar", onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// Captures a fixed safeguard withdrawal and requires an in-person manager signature.
@Composable
internal fun CashWithdrawalAuthorization(
    users: List<LocalUserEntity>,
    repository: PosRepository,
    shift: ShiftEntity,
    onDismiss: () -> Unit,
    onRecorded: (CashWithdrawalEntity) -> Unit,
) {
    val managers = users.filter { it.isManagerOrAdmin }
    var selected by remember { mutableStateOf(managers.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Records the same withdrawal from either keypad enter or the visible action.
    fun record() {
        val cents = Money.fromInput(amount)
        val user = selected
        if (cents == null || cents <= 0) error = "Captura un importe válido."
        else if (user == null) error = "Selecciona un autorizador."
        else scope.launch {
            val withdrawal = withContext(Dispatchers.IO) { repository.recordWithdrawal(shift, cents, user.id, pin) }
            if (withdrawal == null) error = "No se pudo autorizar la sangría." else {
                SyncWorker.enqueue(context)
                onRecorded(withdrawal)
            }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            // Allows the dialog content to scroll when multiple manager profiles are present
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Registrar sangría", style = MaterialTheme.typography.titleLarge)
                Text("Motivo: Resguardo de efectivo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(amount, { amount = it }, label = { Text("Importe en pesos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                managers.forEach { user -> PosButton(user.displayTitle(), { selected = user }, selected = selected?.id == user.id, modifier = Modifier.fillMaxWidth()) }
                Text("PIN de Manager/Superadmin", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Confirmar e imprimir", ::record, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Hides sale data until the cashier responsible for the active shift verifies their PIN.
@Composable
internal fun LockedCashRegister(
    repository: PosRepository,
    cashierId: String,
    cashierName: String,
    onUnlocked: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Reuses the visible action and keypad enter key for the same PIN validation.
    fun unlock() {
        if (busy || pin.isBlank()) return
        scope.launch {
            busy = true
            val valid = withContext(Dispatchers.IO) { repository.authenticate(cashierId, pin) }
            busy = false
            if (valid) onUnlocked() else error = "El PIN no corresponde al cajero responsable."
        }
    }

    // Compact header + full keypad; the whole page scrolls when content is taller than the screen.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).padding(top = 4.dp),
                )
                Text("Pimienta POS", style = MaterialTheme.typography.titleLarge)
                Text("Caja bloqueada", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Turno activo · Cajero responsable: $cashierName",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Agrega PIN para desbloquear",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Numpad(pin, { pin = it }, masked = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                PosButton(
                    label = if (busy) "Desbloqueando…" else "Desbloquear caja",
                    click = ::unlock,
                    enabled = !busy && pin.isNotBlank(),
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "El turno y el carrito continúan resguardados en la tablet.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// Displays one small operational status without presenting it as a dashboard card.
@Composable
internal fun StatusChip(label: String, alert: Boolean = false) {
    val background = if (alert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    val content = if (alert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = background, shape = RoundedCornerShape(8.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Lets portrait users switch between catalog and cart while preserving one draft sale.
@Composable
internal fun PortraitTabs(selected: PortraitPanel, itemCount: Int, onSelect: (PortraitPanel) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosButton("Catálogo", { onSelect(PortraitPanel.CATALOG) }, selected == PortraitPanel.CATALOG, modifier = Modifier.weight(1f))
        PosButton("Carrito · $itemCount", { onSelect(PortraitPanel.CART) }, selected == PortraitPanel.CART, modifier = Modifier.weight(1f))
    }
}

// Shows searchable catalog products as flat touch targets rather than elevated Material cards.
@Composable
internal fun CatalogPanel(
    modifier: Modifier,
    categories: List<String>,
    selectedCategory: String,
    onCategory: (String) -> Unit,
    search: String,
    onSearch: (String) -> Unit,
    products: List<ProductEntity>,
    onProduct: (ProductEntity) -> Unit,
    openAmountEnabled: Boolean = false,
    onOpenAmount: () -> Unit = {},
    openAmountCategories: List<String> = emptyList(),
    onOpenAmountCategory: (String) -> Unit = {},
    onOpenSections: () -> Unit = {},
) {
    val categoryListState = rememberLazyListState()
    val selectedIndex = remember(categories, selectedCategory) {
        categories.indexOf(selectedCategory)
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            categoryListState.animateScrollToItem(maxOf(0, selectedIndex - 1))
        }
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escanea un producto", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                readOnly = true,
                colors = catalogFieldColors(),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PosButton(
                    label = "☰",
                    click = onOpenSections,
                    modifier = Modifier.padding(end = 6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                )
                LazyRow(
                    state = categoryListState,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(categories, key = { it }) { item ->
                        PosButton(
                            label = item,
                            click = { onCategory(item) },
                            selected = selectedCategory == item,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(6.dp))

            val openCategoriesFiltered = remember(openAmountCategories, selectedCategory, search) {
                openAmountCategories.filter { cat ->
                    val matchesCategory = selectedCategory == "Todos" || selectedCategory == "Monto Abierto" || cat.contains(selectedCategory, ignoreCase = true) || selectedCategory.contains(cat, ignoreCase = true)
                    val matchesSearch = search.isBlank() || cat.contains(search, ignoreCase = true)
                    matchesCategory && matchesSearch
                }
            }

            if ((products.isEmpty() && selectedCategory != "Monto Abierto") || (selectedCategory == "Monto Abierto" && openCategoriesFiltered.isEmpty())) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("No hay productos para esta búsqueda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (openAmountEnabled && search.isNotBlank()) {
                            PosButton("¿Agregar como Producto Abierto?", onOpenAmount, primary = true)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (selectedCategory == "Monto Abierto") {
                        items(openCategoriesFiltered) { cat ->
                            OpenProductTile(cat) { onOpenAmountCategory(cat) }
                        }
                    } else {
                        items(products, key = { it.id }) { ProductTile(it) { onProduct(it) } }
                    }
                }
            }
        }
    }
}

// Shows a virtual product tile for open amount categories.
@Composable
internal fun OpenProductTile(category: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("$category (Abierto)", style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Column {
                Text("Capturar precio", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Shows product price in one compact, high-target catalog tile.
@Composable
internal fun ProductTile(product: ProductEntity, onClick: () -> Unit) {
    val unavailable = !product.available

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
        enabled = !unavailable,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Column {
                Text(Money.format(Money.fromCatalog(product.price)), fontWeight = FontWeight.Bold)
                if (unavailable) {
                    Text("No disponible", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
                product.sku.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let { sku ->
                    Text(
                        text = sku,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// Authorizes one fixed-amount discount while preserving the active cashier cart.
@Composable
internal fun DiscountAuthorization(
    users: List<LocalUserEntity>,
    gross: Long,
    current: SaleDiscountDraft?,
    repository: PosRepository,
    onDismiss: () -> Unit,
    onAuthorized: (SaleDiscountDraft) -> Unit,
) {
    val managers = users.filter { it.isManagerOrAdmin }
    var selected by remember { mutableStateOf(managers.firstOrNull()) }
    var amount by remember { mutableStateOf(current?.amountCentavos?.let { BigDecimal.valueOf(it, 2).toPlainString() } ?: "") }
    var reason by remember { mutableStateOf(current?.reason ?: "") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Uses the same validation for the keypad enter key and the primary action.
    fun authorize() {
        val cents = Money.fromInput(amount)
        val user = selected
        when {
            cents == null || cents <= 0 -> error = "Captura un importe válido."
            cents > gross -> error = "El descuento no puede superar la venta."
            reason.isBlank() -> error = "El motivo es obligatorio."
            user == null -> error = "Selecciona un autorizador."
            else -> scope.launch {
                val valid = withContext(Dispatchers.IO) { repository.authenticate(user.id, pin) }
                if (valid) onAuthorized(SaleDiscountDraft(cents, reason.trim(), user)) else error = "El PIN no corresponde al perfil seleccionado."
            }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Descuento / cortesía", style = MaterialTheme.typography.titleLarge)
                Text("Venta bruta: ${Money.format(gross)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(amount, { amount = it }, label = { Text("Importe fijo en pesos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reason, { reason = it }, label = { Text("Motivo obligatorio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                managers.forEach { user -> PosButton(user.displayTitle(), { selected = user }, selected = selected?.id == user.id, modifier = Modifier.fillMaxWidth()) }
                Text("PIN de Manager/Superadmin", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Autorizar", ::authorize, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Displays the editable draft sale and leaves payment to the next workspace state.
@Composable
internal fun CartPanel(
    modifier: Modifier,
    cart: List<CartLine>,
    discount: SaleDiscountDraft?,
    onChange: (List<CartLine>) -> Unit,
    applyDiscount: () -> Unit,
    checkout: () -> Unit,
) {
    val gross = cart.totalCentavos()
    val discountAmount = discount?.amountCentavos ?: 0
    val total = SaleCalculator.netCentavos(gross, discountAmount)
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text("Venta activa", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Folio al confirmar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${cart.sumOf { it.quantity }} artículos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            if (cart.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Agrega productos del catálogo para iniciar una venta.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(cart, key = { it.lineKey }) { line ->
                        val quantityLocked = line.lineType == SaleLineType.OPEN_AMOUNT
                        CartLineRow(
                            line,
                            decrease = {
                                onChange(cart.mapNotNull { current ->
                                    if (current.lineKey != line.lineKey) current
                                    else if (quantityLocked || current.quantity == 1) null
                                    else current.copy(quantity = current.quantity - 1)
                                })
                            },
                            increase = {
                                if (!quantityLocked) {
                                    onChange(cart.map { current ->
                                        if (current.lineKey == line.lineKey) current.copy(quantity = current.quantity + 1) else current
                                    })
                                }
                            },
                            quantityAdjustable = !quantityLocked,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (discount == null) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text(Money.format(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        TotalLine("Venta bruta", Money.format(gross))
                        TotalLine("Descuento · ${discount.reason}", "− ${Money.format(discountAmount)}")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        TotalLine("Total neto", Money.format(total), emphasized = true)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosButton("Limpiar", { onChange(emptyList()) }, enabled = cart.isNotEmpty(), modifier = Modifier.weight(0.8f))
                PosButton("Descuento / cortesía", applyDiscount, enabled = cart.isNotEmpty(), modifier = Modifier.weight(1.2f))
            }
            Spacer(Modifier.height(6.dp))
            PosButton(
                "Cobrar ${Money.format(total)}",
                checkout,
                enabled = cart.isNotEmpty(),
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// Aligns monetary summary labels and values in compact operational rows.
@Composable
private fun TotalLine(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// Keeps quantities inline so a cashier can adjust a line without navigating away.
@Composable
internal fun CartLineRow(
    line: CartLine,
    decrease: () -> Unit,
    increase: () -> Unit,
    quantityAdjustable: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${line.quantity} × ${Money.format(line.unitPriceCentavos)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Money.format(line.subtotalCentavos), fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        QuantityButton("−", decrease)
        Text("${line.quantity}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
        QuantityButton("+", increase, enabled = quantityAdjustable)
    }
}

// Provides a compact touch target for cart quantities without a pill-shaped control.
@Composable
internal fun QuantityButton(label: String, click: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = click,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

// Collects payment in a scrollable center area with fixed cancel and confirm actions.
@Composable
internal fun Checkout(
    modifier: Modifier,
    total: Long,
    courtesy: Boolean,
    busy: Boolean,
    method: PaymentMethod,
    onMethodChanged: (PaymentMethod) -> Unit,
    tendered: String,
    onTenderedChanged: (String) -> Unit,
    back: () -> Unit,
    confirm: (PaymentMethod, Long) -> Unit,
) {
    val tenderedMoney = Money.fromInput(tendered) ?: 0

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PosButton(
                    "← Volver al carrito",
                    back,
                    enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Total a cobrar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(Money.format(total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
                if (!courtesy) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Efectivo", { onMethodChanged(PaymentMethod.CASH) }, method == PaymentMethod.CASH, modifier = Modifier.weight(1f))
                    PosButton("Tarjeta externa", { onMethodChanged(PaymentMethod.EXTERNAL_CARD_MP) }, method == PaymentMethod.EXTERNAL_CARD_MP, modifier = Modifier.weight(1f))
                }
                if (courtesy) {
                    Text("Cortesía autorizada · no se recibe efectivo ni tarjeta", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (method == PaymentMethod.CASH) {
                    CashPayment(tendered, onTenderedChanged, total)
                } else {
                    ExternalCardNotice()
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosButton("Cancelar", back, enabled = !busy, modifier = Modifier.weight(1f))
                PosButton(
                    if (busy) "Confirmando…" else if (courtesy) "Confirmar cortesía" else "Confirmar cobro",
                    { confirm(if (courtesy) PaymentMethod.CORTESIA else method, if (courtesy) 0 else if (method == PaymentMethod.CASH) tenderedMoney else total) },
                    enabled = !busy && (courtesy || method != PaymentMethod.CASH || tenderedMoney >= total),
                    primary = true,
                    modifier = Modifier.weight(1.5f),
                )
            }
        }
    }
}

// Combines numeric entry, denomination shortcuts, and the resulting change for cash.
@Composable
internal fun CashPayment(value: String, changed: (String) -> Unit, total: Long) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val twoColumns = maxWidth >= 480.dp
        if (twoColumns) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CashEntry(Modifier.weight(1f), value, changed, total)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CashEntry(Modifier.fillMaxWidth(), value, changed, total)
            }
        }
    }
}

// Renders direct numeric entry for cash without calling the system keyboard.
@Composable
internal fun CashEntry(modifier: Modifier, value: String, changed: (String) -> Unit, total: Long = 0) {
    val received = Money.fromInput(value) ?: 0
    val change = SaleCalculator.changeCentavos(received, total).coerceAtLeast(0)

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Efectivo recibido", style = MaterialTheme.typography.labelLarge)
            val displayMoney = if (value.isBlank()) Money.format(0) else Money.format(received)
            Text(displayMoney, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (received > total) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cambio a entregar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(Money.format(change), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Spacer(Modifier.height(6.dp))
        Numpad(value, changed)
    }
}

// Explains the manual approval boundary for the external physical terminal.
@Composable
internal fun ExternalCardNotice() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Terminal externa", style = MaterialTheme.typography.titleMedium)
            Text(
                "Confirma este cobro solo después de que Mercado Pago apruebe la operación en su terminal física.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Esta versión no se conecta a la terminal.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// Brief confirmation that auto-hides; a new folio replaces the message and restarts the timer.
@Composable
internal fun SaleCompleted(folio: String?, onFinished: (String) -> Unit) {
    var displayed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(folio) {
        if (folio != null) {
            displayed = folio
            delay(3_500)
            onFinished(folio)
        }
    }
    AnimatedVisibility(
        visible = folio != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Venta confirmada · ${displayed.orEmpty()} · Ticket en cola de impresión",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// Provides number-only input without a nested scroll grid so parent screens can scroll the full pad.
@Composable
internal fun Numpad(
    value: String,
    changed: (String) -> Unit,
    masked: Boolean = false,
    onSubmit: (() -> Unit)? = null,
    revealValue: Boolean = false,
    showEnter: Boolean = onSubmit != null,
) {
    val keys = when {
        masked && showEnter && onSubmit != null -> {
            listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", "⌫", "Entrar")
        }
        masked || (!value.contains('.') && showEnter && onSubmit != null) -> {
            listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "", "0", "⌫")
        }
        else -> {
            listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "⌫")
        }
    }
    // Dots only while typing; empty state stays quiet so the parent can label the field.
    if (masked && !revealValue && value.isNotBlank()) {
        Text(
            "•".repeat(value.length),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.weight(1f).height(48.dp))
                    } else {
                        PosButton(
                            key,
                            {
                                when (key) {
                                    "⌫" -> {
                                        when {
                                            value.endsWith(".00") -> changed(value.dropLast(3))
                                            value.endsWith(".0") -> changed(value.dropLast(2))
                                            else -> changed(value.dropLast(1))
                                        }
                                    }
                                    "Entrar" -> onSubmit?.invoke()
                                    "." -> if (!value.contains('.')) changed(if (value.isBlank()) "0." else "$value.")
                                    else -> {
                                        val fraction = value.substringAfter('.', missingDelimiterValue = "")
                                        val blocksExtraDecimals = value.contains('.') && fraction.length >= 2
                                        if (!blocksExtraDecimals && value.length < 8) changed(value + key)
                                    }
                                }
                            },
                            enabled = key != "Entrar" || (onSubmit != null && value.isNotBlank()),
                            modifier = Modifier.weight(1f).height(48.dp),
                        )
                    }
                }
            }
        }
    }
}

// Sums the immutable price snapshots already captured in the draft cart.
internal fun List<CartLine>.totalCentavos(): Long = SaleCalculator.grossCentavos(this)

// Supplies filled inputs with the POS surface treatment instead of a heavy outline.
@Composable
internal fun catalogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
)

// Standardizes compact, square-cornered actions across the operational workspace.
@Composable
internal fun PosButton(
    label: String,
    click: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
) {
    val emphasized = primary || selected
    Button(
        onClick = click,
        modifier = modifier.heightIn(min = if (contentPadding != null) 36.dp else 42.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = contentPadding ?: ButtonDefaults.ContentPadding,
        border = if (!emphasized) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

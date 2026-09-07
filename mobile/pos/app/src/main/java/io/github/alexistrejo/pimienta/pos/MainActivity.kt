package io.github.alexistrejo.pimienta.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.alexistrejo.pimienta.pos.app.PosApplication
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.domain.CartLine
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.domain.PaymentMethod
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Identifies the visible panel used by portrait tablets during a draft sale.
private enum class PortraitPanel { CATALOG, CART }

// Hosts the offline POS and restores the persisted local state on launch.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = getSharedPreferences("pos-demo", MODE_PRIVATE)
        val repository = PosRepository((application as PosApplication).database)

        setContent {
            var dark by remember {
                mutableStateOf(if (BuildConfig.DEBUG) preferences.getBoolean("dark-theme", true) else true)
            }
            PosTheme(dark) {
                PosApp(repository, dark) { enabled ->
                    dark = enabled
                    preferences.edit().putBoolean("dark-theme", enabled).apply()
                }
            }
        }
    }
}

// Selects the local screen from data persisted in Room rather than from a remote service.
@Composable
private fun PosApp(repository: PosRepository, dark: Boolean, onTheme: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<LocalUserEntity>>(emptyList()) }
    var products by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var shift by remember { mutableStateOf<ShiftEntity?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            val state = withContext(Dispatchers.IO) {
                Triple(repository.users(), repository.products(), repository.activeShift())
            }
            users = state.first
            products = state.second
            shift = state.third
        }
    }

    LaunchedEffect(Unit) { reload() }

    when {
        users.isEmpty() || products.isEmpty() -> Loading(notice, ::reload)
        shift == null -> Access(users, repository, notice, { shift = it }, { notice = it })
        else -> Sale(
            repository = repository,
            shift = shift!!,
            cashier = users.firstOrNull { it.id == shift!!.cashierId }?.displayName ?: "Cajero",
            users = users,
            products = products,
            dark = dark,
            onTheme = onTheme,
        )
    }
}

// Waits for the debug bootstrap without blocking the Compose UI thread.
@Composable
private fun Loading(notice: String?, reload: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Pimienta POS", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(
                notice ?: "Preparando datos locales de demostración…",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            PosButton("Recargar datos locales", reload, primary = true)
        }
    }
}

// Verifies a local PIN and collects the opening cash with the integrated numpad.
@Composable
private fun Access(
    users: List<LocalUserEntity>,
    repository: PosRepository,
    notice: String?,
    opened: (ShiftEntity) -> Unit,
    message: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf(users.first()) }
    var pin by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Abrir turno", style = MaterialTheme.typography.headlineSmall)
                Text("Selecciona tu perfil e ingresa tu PIN.", color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users, key = { it.id }) { profile ->
                        PosButton(profile.displayName, { user = profile }, selected = user.id == profile.id)
                    }
                }

                Text("PIN", style = MaterialTheme.typography.titleMedium)
                Numpad(pin, { pin = it }, masked = true)

                Text("Fondo inicial", style = MaterialTheme.typography.titleMedium)
                Text(Money.format(Money.fromInput(opening) ?: 0), style = MaterialTheme.typography.headlineSmall)
                Numpad(opening, { opening = it })

                notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                PosButton(
                    label = if (busy) "Abriendo turno…" else "Abrir turno",
                    click = {
                        scope.launch {
                            busy = true
                            val result = withContext(Dispatchers.IO) {
                                val cash = Money.fromInput(opening) ?: -1
                                when {
                                    cash < 0 -> Result.failure<ShiftEntity>(IllegalArgumentException("Ingresa un fondo inicial válido."))
                                    !repository.authenticate(user.id, pin) -> Result.failure<ShiftEntity>(IllegalArgumentException("El PIN no corresponde al perfil seleccionado."))
                                    else -> repository.openShift(user.id, cash)?.let { Result.success(it) }
                                        ?: Result.failure(IllegalStateException("No se pudo abrir el turno local."))
                                }
                            }
                            busy = false
                            result.onSuccess(opened).onFailure { message(it.message ?: "No se pudo abrir el turno.") }
                        }
                    },
                    enabled = !busy,
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// Renders a split workspace in landscape and readable alternate panels in portrait.
@Composable
private fun Sale(
    repository: PosRepository,
    shift: ShiftEntity,
    cashier: String,
    users: List<LocalUserEntity>,
    products: List<ProductEntity>,
    dark: Boolean,
    onTheme: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }
    var category by remember { mutableStateOf("Todos") }
    var search by remember { mutableStateOf("") }
    var checkout by remember { mutableStateOf(false) }
    var portraitPanel by remember { mutableStateOf(PortraitPanel.CATALOG) }
    var completedFolio by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var paymentMethodDraft by remember { mutableStateOf(PaymentMethod.CASH) }
    var tenderedDraft by remember { mutableStateOf("") }
    var locked by remember { mutableStateOf(false) }
    var managerAccessRequested by remember { mutableStateOf(false) }
    var manager by remember { mutableStateOf<LocalUserEntity?>(null) }
    val feedbackHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        pending = withContext(Dispatchers.IO) { repository.pendingEvents() }
    }

    val categories = listOf("Todos") + products.map { it.saleCategory }.distinct()
    val filtered = products.filter {
        (category == "Todos" || it.saleCategory == category) &&
            (it.name.contains(search, true) || it.barcode.contains(search, true))
    }

    fun add(product: ProductEntity) {
        if (!product.available || busy) return
        val wasCheckout = checkout
        val line = CartLine(
            productId = product.id,
            name = product.name,
            category = product.saleCategory,
            unitPriceCentavos = Money.fromCatalog(product.price),
            stockPolicy = product.stockPolicy,
            quantity = 1,
        )
        cart = if (cart.none { it.productId == product.id }) cart + line else cart.map {
            if (it.productId == product.id) it.copy(quantity = it.quantity + 1) else it
        }
        if (wasCheckout) {
            checkout = false
            portraitPanel = PortraitPanel.CART
            scope.launch { feedbackHost.showSnackbar("Se agregó ${product.name}. Total actualizado.") }
        }
    }

    fun confirm(method: PaymentMethod, tendered: Long) {
        scope.launch {
            busy = true
            val sale = withContext(Dispatchers.IO) { repository.confirmSale(shift, cart, method, tendered) }
            busy = false
            sale?.let {
                cart = emptyList()
                checkout = false
                portraitPanel = PortraitPanel.CATALOG
                paymentMethodDraft = PaymentMethod.CASH
                tenderedDraft = ""
                completedFolio = it.folio
                pending = withContext(Dispatchers.IO) { repository.pendingEvents() }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding(),
    ) {
        val landscape = maxWidth > maxHeight
        if (locked) {
            LockedCashRegister(
                repository = repository,
                cashierId = shift.cashierId,
                cashierName = cashier,
                onUnlocked = { locked = false },
            )
        } else if (manager != null) {
            ManagerPanel(
                shift = shift,
                manager = manager!!,
                products = products,
                pendingEvents = pending,
                onReturnToSale = { manager = null },
            )
        } else Column(Modifier.fillMaxSize()) {
            StatusBar(
                cashier = cashier,
                pending = pending,
                dark = dark,
                onTheme = onTheme,
                landscape = landscape,
                lockCashRegister = {
                    checkout = false
                    paymentMethodDraft = PaymentMethod.CASH
                    tenderedDraft = ""
                    locked = true
                },
                openManager = { managerAccessRequested = true },
            )
            completedFolio?.let { SaleCompleted(it) { completedFolio = null } }

            if (landscape) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    CatalogPanel(
                        Modifier.weight(0.6f).fillMaxHeight(), categories, category, { category = it }, search,
                        { search = it }, filtered, ::add,
                    )
                    if (checkout) {
                        Checkout(
                            modifier = Modifier.weight(0.4f).fillMaxHeight().padding(12.dp),
                            total = cart.totalCentavos(),
                            busy = busy,
                            method = paymentMethodDraft,
                            onMethodChanged = { paymentMethodDraft = it },
                            tendered = tenderedDraft,
                            onTenderedChanged = { tenderedDraft = it },
                            back = { checkout = false },
                            confirm = ::confirm,
                        )
                    } else {
                        CartPanel(Modifier.weight(0.4f).fillMaxHeight(), cart, { cart = it }) { checkout = true }
                    }
                }
            } else if (checkout) {
                Checkout(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                    total = cart.totalCentavos(),
                    busy = busy,
                    method = paymentMethodDraft,
                    onMethodChanged = { paymentMethodDraft = it },
                    tendered = tenderedDraft,
                    onTenderedChanged = { tenderedDraft = it },
                    back = { checkout = false },
                    confirm = ::confirm,
                )
            } else {
                PortraitTabs(portraitPanel, cart.sumOf { it.quantity }) { portraitPanel = it }
                if (portraitPanel == PortraitPanel.CATALOG) {
                    CatalogPanel(
                        Modifier.weight(1f).fillMaxWidth(), categories, category, { category = it }, search,
                        { search = it }, filtered, ::add,
                    )
                } else {
                    CartPanel(Modifier.weight(1f).fillMaxWidth(), cart, { cart = it }) { checkout = true }
                }
            }
        }

        if (managerAccessRequested) {
            ManagerAccess(
                users = users,
                repository = repository,
                onDismiss = { managerAccessRequested = false },
                onAuthorized = {
                    manager = it
                    managerAccessRequested = false
                },
            )
        }
        SnackbarHost(
            hostState = feedbackHost,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}

// Keeps operational status visible without letting a portrait header overflow.
@Composable
private fun StatusBar(
    cashier: String,
    pending: Int,
    dark: Boolean,
    onTheme: (Boolean) -> Unit,
    landscape: Boolean,
    lockCashRegister: () -> Unit,
    openManager: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pimienta POS", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            PosButton("Bloquear caja", lockCashRegister)
            Spacer(Modifier.width(8.dp))
            PosButton("Panel Manager", openManager)
            Spacer(Modifier.width(8.dp))
            if (BuildConfig.DEBUG) {
                PosButton(if (dark) "Tema claro" else "Tema oscuro", { onTheme(!dark) })
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusChip("Sin conexión · $pending pendientes")
            StatusChip("Tablet T1")
            StatusChip(if (landscape) "Turno abierto · $cashier" else "Turno abierto")
            StatusChip("Impresora lista")
        }
    }
}

// Hides sale data until the cashier responsible for the active shift verifies their PIN.
@Composable
private fun LockedCashRegister(
    repository: PosRepository,
    cashierId: String,
    cashierName: String,
    onUnlocked: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Pimienta POS", style = MaterialTheme.typography.headlineSmall)
                Text("Caja bloqueada", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Turno activo · Cajero responsable: $cashierName",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("PIN", style = MaterialTheme.typography.titleMedium)
                Numpad(pin, { pin = it }, masked = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                PosButton(
                    label = if (busy) "Desbloqueando…" else "Desbloquear caja",
                    click = {
                        scope.launch {
                            busy = true
                            val valid = withContext(Dispatchers.IO) { repository.authenticate(cashierId, pin) }
                            busy = false
                            if (valid) onUnlocked() else error = "El PIN no corresponde al cajero responsable."
                        }
                    },
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
            }
        }
    }
}

// Displays one small operational status without presenting it as a dashboard card.
@Composable
private fun StatusChip(label: String) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

// Lets portrait users switch between catalog and cart while preserving one draft sale.
@Composable
private fun PortraitTabs(selected: PortraitPanel, itemCount: Int, onSelect: (PortraitPanel) -> Unit) {
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
private fun CatalogPanel(
    modifier: Modifier,
    categories: List<String>,
    selectedCategory: String,
    onCategory: (String) -> Unit,
    search: String,
    onSearch: (String) -> Unit,
    products: List<ProductEntity>,
    onProduct: (ProductEntity) -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text("Catálogo", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por nombre o código") },
                singleLine = true,
                colors = catalogFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it }) { item ->
                    PosButton(item, { onCategory(item) }, selected = selectedCategory == item)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(10.dp))

            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay productos para esta búsqueda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(products, key = { it.id }) { ProductTile(it) { onProduct(it) } }
                }
            }
        }
    }
}

// Shows product price and stock context in one compact, high-target catalog tile.
@Composable
private fun ProductTile(product: ProductEntity, onClick: () -> Unit) {
    val unavailable = !product.available
    val stock = product.stock.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val controlled = product.stockPolicy == "CONTROLLED"
    val warning = controlled && stock <= BigDecimal.ZERO
    val status = when {
        unavailable -> "No disponible"
        !controlled -> "Preparado al momento"
        warning -> "Inventario local: ${product.stock}"
        else -> "${product.stock} disponibles"
    }
    val statusColor = if (unavailable || warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 116.dp),
        enabled = !unavailable,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Column {
                Text(Money.format(Money.fromCatalog(product.price)), fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.labelMedium, color = statusColor)
            }
        }
    }
}

// Displays the editable draft sale and leaves payment to the next workspace state.
@Composable
private fun CartPanel(
    modifier: Modifier,
    cart: List<CartLine>,
    onChange: (List<CartLine>) -> Unit,
    checkout: () -> Unit,
) {
    val total = cart.totalCentavos()
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Venta activa", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Folio al confirmar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${cart.sumOf { it.quantity }} artículos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
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
                    items(cart, key = { it.productId }) { line ->
                        CartLineRow(
                            line,
                            decrease = {
                                onChange(cart.mapNotNull { current ->
                                    if (current.productId != line.productId) current
                                    else if (current.quantity == 1) null
                                    else current.copy(quantity = current.quantity - 1)
                                })
                            },
                            increase = {
                                onChange(cart.map { current ->
                                    if (current.productId == line.productId) current.copy(quantity = current.quantity + 1) else current
                                })
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total", style = MaterialTheme.typography.titleLarge)
                Text(Money.format(total), style = MaterialTheme.typography.titleLarge)
            }
            PosButton("Limpiar carrito", { onChange(emptyList()) }, enabled = cart.isNotEmpty(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
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

// Keeps quantities inline so a cashier can adjust a line without navigating away.
@Composable
private fun CartLineRow(line: CartLine, decrease: () -> Unit, increase: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${line.quantity} × ${Money.format(line.unitPriceCentavos)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Money.format(line.unitPriceCentavos * line.quantity), fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        QuantityButton("−", decrease)
        Text("${line.quantity}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
        QuantityButton("+", increase)
    }
}

// Provides a compact touch target for cart quantities without a pill-shaped control.
@Composable
private fun QuantityButton(label: String, click: () -> Unit) {
    Button(
        onClick = click,
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
private fun Checkout(
    modifier: Modifier,
    total: Long,
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
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            PosButton("Volver al carrito", back, enabled = !busy, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Cobro", style = MaterialTheme.typography.titleLarge)
                Text("Total a cobrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.format(total), style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Efectivo", { onMethodChanged(PaymentMethod.CASH) }, method == PaymentMethod.CASH, modifier = Modifier.weight(1f))
                    PosButton("Tarjeta externa", { onMethodChanged(PaymentMethod.EXTERNAL_CARD_MP) }, method == PaymentMethod.EXTERNAL_CARD_MP, modifier = Modifier.weight(1f))
                }
                if (method == PaymentMethod.CASH) CashPayment(tendered, onTenderedChanged, total) else ExternalCardNotice()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosButton("Cancelar intento", back, enabled = !busy, modifier = Modifier.weight(1f))
                PosButton(
                    if (busy) "Confirmando…" else "Confirmar cobro e imprimir",
                    { confirm(method, if (method == PaymentMethod.CASH) tenderedMoney else total) },
                    enabled = !busy && (method != PaymentMethod.CASH || tenderedMoney >= total),
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// Combines numeric entry, denomination shortcuts, and the resulting change for cash.
@Composable
private fun CashPayment(value: String, changed: (String) -> Unit, total: Long) {
    val received = Money.fromInput(value) ?: 0
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val twoColumns = maxWidth >= 480.dp
        if (twoColumns) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CashEntry(Modifier.weight(1f), value, changed)
                CashShortcuts(Modifier.weight(1f), received, total, changed)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CashEntry(Modifier.fillMaxWidth(), value, changed)
                CashShortcuts(Modifier.fillMaxWidth(), received, total, changed)
            }
        }
    }
}

// Renders direct numeric entry for cash without calling the system keyboard.
@Composable
private fun CashEntry(modifier: Modifier, value: String, changed: (String) -> Unit) {
    Column(modifier) {
        Text("Efectivo recibido", style = MaterialTheme.typography.labelLarge)
        Text(Money.format(Money.fromInput(value) ?: 0), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Numpad(value, changed)
    }
}

// Adds common Mexican denominations and exposes the calculated change.
@Composable
private fun CashShortcuts(modifier: Modifier, received: Long, total: Long, changed: (String) -> Unit) {
    Column(modifier) {
        Text("Billetes y monedas", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        DenominationGrid(
            add = { denomination -> changed(moneyInput(received + denomination)) },
            exact = { changed(moneyInput(total)) },
        )
        Spacer(Modifier.height(12.dp))
        ChangeSummary((received - total).coerceAtLeast(0))
    }
}

// Explains the manual approval boundary for the external physical terminal.
@Composable
private fun ExternalCardNotice() {
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

// Presents denomination shortcuts that add to the received cash amount.
@Composable
private fun DenominationGrid(add: (Long) -> Unit, exact: () -> Unit) {
    val denominations = listOf(50L, 100L, 500L, 1_000L, 2_000L, 5_000L, 10_000L, 20_000L, 50_000L)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(172.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(denominations) { denomination ->
            PosButton(Money.format(denomination), { add(denomination) }, modifier = Modifier.fillMaxWidth())
        }
        item { PosButton("Exacto", exact, modifier = Modifier.fillMaxWidth()) }
    }
}

// Highlights change in a flat operational container rather than a modal confirmation.
@Composable
private fun ChangeSummary(change: Long) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Cambio a entregar", style = MaterialTheme.typography.labelLarge)
            Text(Money.format(change), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// Announces a committed local sale without hiding the workspace behind a dialog.
@Composable
private fun SaleCompleted(folio: String, dismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Venta confirmada · $folio · Ticket en cola de impresión",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
        Button(
            onClick = dismiss,
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("Nueva venta")
        }
    }
}

// Provides number-only input without invoking the Android keyboard during cashier workflows.
@Composable
internal fun Numpad(value: String, changed: (String) -> Unit, masked: Boolean = false) {
    val keys = listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "⌫")
    if (masked) {
        Text(if (value.isBlank()) "••••" else "•".repeat(value.length), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(218.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items(keys) { key ->
            PosButton(
                key,
                {
                    when (key) {
                        "⌫" -> changed(value.dropLast(1))
                        "." -> if (!value.contains('.')) changed(if (value.isBlank()) "0." else "$value.")
                        else -> if (value.length < 8) changed(value + key)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            )
        }
    }
}

// Formats centavos as a numeric value that can be read back by Money.fromInput.
private fun moneyInput(centavos: Long): String = BigDecimal.valueOf(centavos, 2).stripTrailingZeros().toPlainString()

// Sums the immutable price snapshots already captured in the draft cart.
private fun List<CartLine>.totalCentavos(): Long = sumOf { it.unitPriceCentavos * it.quantity }

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
) {
    val emphasized = primary || selected
    Button(
        onClick = click,
        modifier = modifier.heightIn(min = 42.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
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

package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.hardware.BarcodeScanner
import io.github.alexistrejo.pimienta.pos.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Renders a split workspace in landscape and readable alternate panels in portrait.
@Composable
internal fun Sale(
    repository: PosRepository,
    shift: ShiftEntity,
    cashier: String,
    users: List<LocalUserEntity>,
    products: List<ProductEntity>,
    policy: PosPolicyEntity? = null,
    syncState: SyncStateEntity? = null,
    dark: Boolean,
    onTheme: (Boolean) -> Unit,
    onShiftClosed: () -> Unit,
    scanner: BarcodeScanner? = null,
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
    var discount by remember { mutableStateOf<SaleDiscountDraft?>(null) }
    var discountRequested by remember { mutableStateOf(false) }
    var withdrawalRequested by remember { mutableStateOf(false) }
    var pendingCatalogBarcode by remember { mutableStateOf<String?>(null) }
    var openAmountRequested by remember { mutableStateOf(false) }
    val feedbackHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val mode = repository.mode()
    val (printerLabel, printerAlert) = rememberLivePrinterStatus(context, mode)
    val openAmountAllowed = policy?.allowOpenProducts ?: repository.allowOpenProducts()
    val openAmountCategories = remember(policy) {
        policy?.let {
            runCatching {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(it.openAmountCategoriesJson)
            }.getOrDefault(emptyList())
        } ?: repository.openAmountCategories()
    }

    LaunchedEffect(repository) {
        repository.observePendingEvents().collect { pending = it }
    }

    val categories = listOf("Todos") + products.map { it.saleCategory }.distinct()
    val filtered = products.filter {
        (category == "Todos" || it.saleCategory == category) &&
            (it.name.contains(search, true) || it.sku.contains(search, true) || it.barcode?.contains(search, true) == true)
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
            lineType = SaleLineType.CATALOG,
        )
        cart = if (cart.none { it.lineType == SaleLineType.CATALOG && it.productId == product.id }) {
            cart + line
        } else {
            cart.map {
                if (it.lineType == SaleLineType.CATALOG && it.productId == product.id) it.copy(quantity = it.quantity + 1) else it
            }
        }
        discount = null
        if (wasCheckout) {
            checkout = false
            portraitPanel = PortraitPanel.CART
            scope.launch { feedbackHost.showSnackbar("Se agregó ${product.name}. Total actualizado.") }
        }
    }

    fun addPendingCatalogLine(barcode: String, centavos: Long) {
        if (busy || centavos <= 0) return
        val wasCheckout = checkout
        val lineKey = "pending:$barcode:$centavos"
        val line = CartLine(
            productId = null,
            name = "Producto pendiente de catálogo · $barcode",
            category = "Pendiente de catálogo",
            unitPriceCentavos = centavos,
            stockPolicy = "UNLIMITED",
            quantity = 1,
            lineType = SaleLineType.PENDING_CATALOG,
            sourceBarcode = barcode,
        )
        cart = if (cart.any { it.lineKey == lineKey }) {
            cart.map { if (it.lineKey == lineKey) it.copy(quantity = it.quantity + 1) else it }
        } else {
            cart + line
        }
        discount = null
        pendingCatalogBarcode = null
        if (wasCheckout) {
            checkout = false
            portraitPanel = PortraitPanel.CART
            scope.launch { feedbackHost.showSnackbar("Se agregó producto pendiente. Total actualizado.") }
        }
    }

    // Verifies the manager PIN before adding the auditable open amount line.
    fun addOpenAmountLine(category: String, centavos: Long, authorizer: LocalUserEntity, pin: String) {
        if (busy || !openAmountAllowed) return
        scope.launch {
            val approved = withContext(Dispatchers.IO) {
                authorizer.active &&
                    (authorizer.role == "MANAGER" || authorizer.role == "SUPERADMIN") &&
                    authorizer.id.toLongOrNull() != null &&
                    repository.authenticate(authorizer.id, pin)
            }
            if (!approved) {
                feedbackHost.showSnackbar("PIN inválido o autorizador inactivo.")
                return@launch
            }
            cart = cart + CartLine(
                null,
                "Producto abierto · ${category.trim()}",
                category.trim(),
                centavos,
                "NOT_CONTROLLED",
                1,
                SaleLineType.OPEN_AMOUNT,
                null,
                authorizer.id.toLong(),
                System.currentTimeMillis(),
            )
            openAmountRequested = false
            discount = null
        }
    }

    // Starts the scanner and routes reads through the same catalog resolver as search.
    LaunchedEffect(scanner) {
        scanner ?: return@LaunchedEffect
        scanner.start()
        scanner.events.collect { read ->
            if (busy) return@collect
            if (checkout) {
                checkout = false
                paymentMethodDraft = PaymentMethod.CASH
                tenderedDraft = ""
                portraitPanel = PortraitPanel.CART
            }
            val code = read.rawValue.trim()
            if (code.isBlank()) return@collect
            val product = withContext(Dispatchers.IO) { repository.findProductByCode(code) }
            when {
                product == null -> pendingCatalogBarcode = code
                !product.available -> scope.launch { feedbackHost.showSnackbar("${product.name} no está disponible.") }
                else -> add(product)
            }
        }
    }

    fun confirm(method: PaymentMethod, tendered: Long) {
        scope.launch {
            busy = true
            val sale = withContext(Dispatchers.IO) { repository.confirmSale(shift, cart, method, tendered, discount) }
            busy = false
            sale?.let {
                cart = emptyList()
                checkout = false
                portraitPanel = PortraitPanel.CATALOG
                paymentMethodDraft = PaymentMethod.CASH
                tenderedDraft = ""
                discount = null
                completedFolio = it.folio
                pending = withContext(Dispatchers.IO) { repository.pendingEvents() }
                PrintWorker.enqueue(context)
                SyncWorker.enqueue(context)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
                repository = repository,
                onReturnToSale = { manager = null },
                onShiftClosed = onShiftClosed,
            )
        } else Column(Modifier.fillMaxSize()) {
            StatusBar(
                cashier = cashier,
                pending = pending,
                syncLabel = inventorySyncLabel(syncState, pending),
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
                openWithdrawal = { withdrawalRequested = true },
                withdrawalEnabled = !checkout && !busy,
                printerLabel = printerLabel,
                printerAlert = printerAlert,
                scannerLabel = "Lector HID",
            )
            SaleCompleted(completedFolio) { folio ->
                if (completedFolio == folio) completedFolio = null
            }
            if (discountRequested) {
                DiscountAuthorization(
                    users = users,
                    gross = cart.totalCentavos(),
                    current = discount,
                    repository = repository,
                    onDismiss = { discountRequested = false },
                    onAuthorized = { discount = it; discountRequested = false },
                )
            }
            if (withdrawalRequested) {
                CashWithdrawalAuthorization(
                    users = users,
                    repository = repository,
                    shift = shift,
                    onDismiss = { withdrawalRequested = false },
                    onRecorded = { withdrawalRequested = false; scope.launch { feedbackHost.showSnackbar("Sangría ${it.folio} registrada; comprobante en cola.") } },
                )
            }
            pendingCatalogBarcode?.let { barcode ->
                PendingCatalogDialog(
                    barcode = barcode,
                    openAmountAvailable = openAmountAllowed,
                    onDismiss = { pendingCatalogBarcode = null },
                    onConfirm = { addPendingCatalogLine(barcode, it) },
                    onOpenAmount = { pendingCatalogBarcode = null; openAmountRequested = true },
                )
            }
            if (openAmountRequested) {
                OpenAmountDialog(
                    categories = openAmountCategories,
                    users = users,
                    verifyPin = { user, pin ->
                        withContext(Dispatchers.IO) {
                            user.active &&
                                (user.role == "MANAGER" || user.role == "SUPERADMIN") &&
                                user.id.toLongOrNull() != null &&
                                repository.authenticate(user.id, pin)
                        }
                    },
                    onDismiss = { openAmountRequested = false },
                    onConfirm = ::addOpenAmountLine,
                )
            }

            if (landscape) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                        CatalogPanel(
                            Modifier.weight(0.6f).fillMaxHeight(), categories, category, { category = it }, search,
                            { search = it }, filtered, ::add, openAmountAllowed, { openAmountRequested = true },
                        )
                    if (checkout) {
                        Checkout(
                            modifier = Modifier.weight(0.4f).fillMaxHeight().padding(12.dp),
                            total = cart.totalCentavos() - (discount?.amountCentavos ?: 0),
                            courtesy = discount?.amountCentavos == cart.totalCentavos() && cart.isNotEmpty(),
                            busy = busy,
                            method = paymentMethodDraft,
                            onMethodChanged = { paymentMethodDraft = it },
                            tendered = tenderedDraft,
                            onTenderedChanged = { tenderedDraft = it },
                            back = { checkout = false },
                            confirm = ::confirm,
                        )
                    } else {
                        CartPanel(Modifier.weight(0.4f).fillMaxHeight(), cart, discount, { cart = it; discount = null }, { discountRequested = true }) { checkout = true }
                    }
                }
            } else if (checkout) {
                Checkout(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                    total = cart.totalCentavos() - (discount?.amountCentavos ?: 0),
                    courtesy = discount?.amountCentavos == cart.totalCentavos() && cart.isNotEmpty(),
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
                        { search = it }, filtered, ::add, openAmountAllowed, { openAmountRequested = true },
                    )
                } else {
                    CartPanel(Modifier.weight(1f).fillMaxWidth(), cart, discount, { cart = it; discount = null }, { discountRequested = true }) { checkout = true }
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

// Describes stock freshness without claiming that an offline tablet has global real-time stock.
internal fun inventorySyncLabel(state: SyncStateEntity?, pending: Int, now: Long = System.currentTimeMillis()): String {
    val pendingText = if (pending == 1) "1 cambio local pendiente" else "$pending cambios locales pendientes"
    if (state == null) return "Inventario local · $pendingText"
    if (state.status != "ONLINE" || state.lastSuccessfulAtEpochMillis == null) {
        return "Inventario puede estar desactualizado · $pendingText"
    }
    val minutes = ((now - state.lastSuccessfulAtEpochMillis).coerceAtLeast(0) / 60_000)
    return if (pending > 0) {
        "Base sincronizada hace ${minutes} min · $pendingText"
    } else {
        "Inventario sincronizado hace ${minutes} min"
    }
}

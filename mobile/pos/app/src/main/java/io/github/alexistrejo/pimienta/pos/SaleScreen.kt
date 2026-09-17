package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
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
import kotlinx.serialization.json.Json

// Serializes and restores the cart list across activity state saves.
private val CartListSaver: Saver<MutableState<List<CartLine>>, String> = Saver(
    save = { Json.encodeToString(it.value) },
    restore = {
        mutableStateOf(
            runCatching { Json.decodeFromString<List<CartLine>>(it) }
                .getOrDefault(emptyList())
        )
    }
)

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
    var cart by rememberSaveable(saver = CartListSaver) { mutableStateOf<List<CartLine>>(emptyList()) }
    var category by rememberSaveable { mutableStateOf("Todos") }
    var search by rememberSaveable { mutableStateOf("") }
    var checkout by rememberSaveable { mutableStateOf(false) }
    var portraitPanel by rememberSaveable { mutableStateOf(PortraitPanel.CATALOG) }
    var completedFolio by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var paymentMethodDraft by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
    var tenderedDraft by rememberSaveable { mutableStateOf("") }
    var locked by rememberSaveable { mutableStateOf(false) }
    var managerAccessRequested by rememberSaveable { mutableStateOf(false) }
    var managerId by rememberSaveable { mutableStateOf<String?>(null) }
    val manager = remember(managerId, users) { users.firstOrNull { it.id == managerId } }
    var discountAmountCentavos by rememberSaveable { mutableStateOf<Long?>(null) }
    var discountReason by rememberSaveable { mutableStateOf("") }
    var discountAuthorizedById by rememberSaveable { mutableStateOf<String?>(null) }
    val discount = remember(discountAmountCentavos, discountReason, discountAuthorizedById, users) {
        val amount = discountAmountCentavos ?: return@remember null
        val authorizer = users.firstOrNull { it.id == discountAuthorizedById } ?: return@remember null
        SaleDiscountDraft(amount, discountReason, authorizer)
    }
    fun updateDiscount(draft: SaleDiscountDraft?) {
        if (draft == null) {
            discountAmountCentavos = null
            discountReason = ""
            discountAuthorizedById = null
        } else {
            discountAmountCentavos = draft.amountCentavos
            discountReason = draft.reason
            discountAuthorizedById = draft.authorizedBy.id
        }
    }
    var discountRequested by rememberSaveable { mutableStateOf(false) }
    var withdrawalRequested by rememberSaveable { mutableStateOf(false) }
    var pendingCatalogBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var openAmountRequested by rememberSaveable { mutableStateOf(false) }
    var selectedOpenCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var sectionsRequested by rememberSaveable { mutableStateOf(false) }
    val feedbackHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val mode = repository.mode()
    val (printerLabel, printerAlert) = rememberLivePrinterStatus(context, mode)
    val openAmountAllowed = policy?.allowOpenProducts ?: false
    val openAmountCategories = remember(policy) {
        policy?.let {
            runCatching {
                Json.decodeFromString<List<String>>(it.openAmountCategoriesJson)
            }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    LaunchedEffect(repository) {
        repository.observePendingEvents().collect { pending = it }
    }

    val categories = remember(products, openAmountAllowed) {
        val base = listOf("Todos") + products.map { it.saleCategory }.distinct()
        if (openAmountAllowed) base + "Monto Abierto" else base
    }
    val filtered = products.filter {
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
        updateDiscount(null)
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
        updateDiscount(null)
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
            updateDiscount(null)
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
            search = ""
            val product = withContext(Dispatchers.IO) { repository.findProductByCode(code) }
            when {
                product == null -> pendingCatalogBarcode = code
                !product.available -> scope.launch { feedbackHost.showSnackbar("${product.name} no está disponible.") }
                else -> add(product)
            }
        }
    }

    // Submits manual search queries on Enter, requiring an exact SKU, barcode or name match.
    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        scope.launch {
            val product = withContext(Dispatchers.IO) { repository.findProductByCode(trimmed) }
                ?: products.firstOrNull {
                    it.sku.equals(trimmed, ignoreCase = true) ||
                    it.barcode?.equals(trimmed, ignoreCase = true) == true ||
                    it.legacyBarcode?.equals(trimmed, ignoreCase = true) == true ||
                    it.name.equals(trimmed, ignoreCase = true)
                }
            search = ""
            when {
                product == null -> pendingCatalogBarcode = trimmed
                !product.available -> feedbackHost.showSnackbar("${product.name} no está disponible.")
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
                updateDiscount(null)
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
                manager = manager,
                products = products,
                pendingEvents = pending,
                repository = repository,
                onReturnToSale = { managerId = null },
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
                    onAuthorized = { updateDiscount(it); discountRequested = false },
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
                    onDismiss = { 
                        openAmountRequested = false
                        selectedOpenCategory = null
                    },
                    onConfirm = ::addOpenAmountLine,
                    initialCategory = selectedOpenCategory
                )
            }

            if (sectionsRequested) {
                SectionsDialog(
                    categories = categories,
                    selected = category,
                    onDismiss = { sectionsRequested = false },
                    onSelect = { category = it }
                )
            }

            if (landscape) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                        CatalogPanel(
                            modifier = Modifier.weight(0.6f).fillMaxHeight(),
                            categories = categories,
                            selectedCategory = category,
                            onCategory = { category = it },
                            search = search,
                            onSearch = { search = it },
                            products = filtered,
                            onProduct = ::add,
                            openAmountEnabled = openAmountAllowed,
                            onOpenAmount = { pendingCatalogBarcode = null; openAmountRequested = true },
                            openAmountCategories = openAmountCategories,
                            onOpenAmountCategory = { cat ->
                                selectedOpenCategory = cat
                                openAmountRequested = true
                            },
                            onOpenSections = { sectionsRequested = true },
                            onSubmitSearch = ::submitSearch,
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
                        CartPanel(Modifier.weight(0.4f).fillMaxHeight(), cart, discount, { cart = it; updateDiscount(null) }, { discountRequested = true }) { checkout = true }
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
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        categories = categories,
                        selectedCategory = category,
                        onCategory = { category = it },
                        search = search,
                        onSearch = { search = it },
                        products = filtered,
                        onProduct = ::add,
                        openAmountEnabled = openAmountAllowed,
                        onOpenAmount = { pendingCatalogBarcode = null; openAmountRequested = true },
                        openAmountCategories = openAmountCategories,
                        onOpenAmountCategory = { cat ->
                            selectedOpenCategory = cat
                            openAmountRequested = true
                        },
                        onOpenSections = { sectionsRequested = true },
                        onSubmitSearch = ::submitSearch,
                    )
                } else {
                    CartPanel(Modifier.weight(1f).fillMaxWidth(), cart, discount, { cart = it; updateDiscount(null) }, { discountRequested = true }) { checkout = true }
                }
            }
        }

        if (managerAccessRequested) {
            ManagerAccess(
                users = users,
                repository = repository,
                onDismiss = { managerAccessRequested = false },
                onAuthorized = {
                    managerId = it.id
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

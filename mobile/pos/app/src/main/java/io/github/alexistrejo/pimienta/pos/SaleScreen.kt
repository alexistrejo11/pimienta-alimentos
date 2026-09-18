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
import io.github.alexistrejo.pimienta.pos.app.PosApplication
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.data.sync.PosApiUserMessages
import io.github.alexistrejo.pimienta.pos.data.sync.ProvisioningRepository
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.data.sync.runForegroundSync
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
    var createProductRequested by rememberSaveable { mutableStateOf(false) }
    var createProductLockedBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var createProductBusy by remember { mutableStateOf(false) }
    var createProductError by remember { mutableStateOf<String?>(null) }
    var syncBusy by remember { mutableStateOf(false) }
    var saleCategories by remember { mutableStateOf(emptyList<String>()) }
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
    LaunchedEffect(products) {
        saleCategories = withContext(Dispatchers.IO) {
            repository.saleCategoryNames().ifEmpty { products.map { it.saleCategory }.filter { it.isNotBlank() }.distinct() }
        }
    }

    val categories = remember(products, openAmountAllowed) {
        val base = listOf("Todos") + products.map { it.saleCategory }.distinct()
        if (openAmountAllowed) base + "Monto Abierto" else base
    }
    val filtered = remember(products, category, search) {
        products.filter { product ->
            val matchesCategory = category == "Todos" || category == "Monto Abierto" || product.saleCategory.equals(category, ignoreCase = true)
            val matchesSearch = search.isBlank() ||
                product.name.contains(search, ignoreCase = true) ||
                product.sku.contains(search, ignoreCase = true) ||
                product.barcode?.contains(search, ignoreCase = true) == true
            matchesCategory && matchesSearch
        }
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

    fun submitCreatedProduct(name: String, category: String, priceCentavos: Long, barcode: String?, controlled: Boolean) {
        val operatorId = shift.cashierId.toLongOrNull()
        val app = context.applicationContext as PosApplication
        val training = repository.mode() != io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode.PRODUCTION
        scope.launch {
            createProductBusy = true
            createProductError = null
            val result = withContext(Dispatchers.IO) {
                if (training) {
                    repository.createTrainingProduct(name, priceCentavos, category, barcode, controlled)
                } else {
                    ProvisioningRepository(context, app.databaseProvider).createProduct(
                        name, priceCentavos, category, barcode, operatorId, controlled,
                    )
                }
            }
            createProductBusy = false
            result.fold(
                onSuccess = { product ->
                    createProductRequested = false
                    createProductLockedBarcode = null
                    pendingCatalogBarcode = null
                    add(product)
                    scope.launch { feedbackHost.showSnackbar("${product.name} guardado en catálogo.") }
                },
                onFailure = {
                    createProductError = if (training) {
                        it.message ?: "No se pudo guardar el producto de práctica."
                    } else {
                        PosApiUserMessages.from(it)
                    }
                },
            )
        }
    }

    fun syncCatalogNow() {
        if (syncBusy) return
        scope.launch {
            syncBusy = true
            val message = withContext(Dispatchers.IO) {
                if (repository.mode() != io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode.PRODUCTION) {
                    "Capacitación: no hay servidor. El producto queda en esta tablet."
                } else {
                    runForegroundSync(context)
                }
            }
            syncBusy = false
            feedbackHost.showSnackbar(message)
        }
    }

    // Adds an open amount line to the cart without requiring manager PIN authorization.
    fun addOpenAmountLine(category: String, centavos: Long) {
        if (busy || !openAmountAllowed || centavos <= 0) return
        cart = cart + CartLine(
            null,
            "Producto abierto · ${category.trim()}",
            category.trim(),
            centavos,
            "NOT_CONTROLLED",
            1,
            SaleLineType.OPEN_AMOUNT,
            null,
            null,
            System.currentTimeMillis(),
        )
        openAmountRequested = false
        updateDiscount(null)
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
                users = users,
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
                onCreateProduct = {
                    createProductLockedBarcode = null
                    createProductError = null
                    createProductRequested = true
                },
                onSyncNow = ::syncCatalogNow,
                syncBusy = syncBusy,
                createProductEnabled = !createProductBusy,
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
                    onSaveToCatalog = {
                        createProductLockedBarcode = barcode
                        createProductRequested = true
                    },
                    onOpenAmount = { pendingCatalogBarcode = null; openAmountRequested = true },
                )
            }
            if (createProductRequested) {
                CreatePosProductDialog(
                    categories = saleCategories,
                    lockedBarcode = createProductLockedBarcode,
                    sandbox = repository.mode() != io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode.PRODUCTION,
                    busy = createProductBusy,
                    error = createProductError,
                    onDismiss = {
                        createProductRequested = false
                        createProductLockedBarcode = null
                        createProductError = null
                    },
                    onSubmit = ::submitCreatedProduct,
                )
            }
            if (openAmountRequested) {
                OpenAmountDialog(
                    categories = openAmountCategories,
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

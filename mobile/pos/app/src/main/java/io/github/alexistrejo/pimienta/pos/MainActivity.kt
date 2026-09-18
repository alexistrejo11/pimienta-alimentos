package io.github.alexistrejo.pimienta.pos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexistrejo.pimienta.pos.app.PosApplication
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.domain.TrainingModePolicy
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.compose.ui.platform.LocalContext
import io.github.alexistrejo.pimienta.pos.data.sync.DeviceCredentials
import io.github.alexistrejo.pimienta.pos.data.sync.DeviceSessionPolicy
import io.github.alexistrejo.pimienta.pos.data.sync.PosApiUserMessages
import io.github.alexistrejo.pimienta.pos.data.sync.ProvisioningRepository
import io.github.alexistrejo.pimienta.pos.data.sync.PRODUCTION_API_URL
import io.github.alexistrejo.pimienta.pos.data.sync.runForegroundSync
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.hardware.BarcodeScanner
import io.github.alexistrejo.pimienta.pos.hardware.FakeBarcodeScanner
import io.github.alexistrejo.pimienta.pos.hardware.HidKeyboardBarcodeScanner
import io.github.alexistrejo.pimienta.pos.hardware.MultiplexBarcodeScanner
import io.github.alexistrejo.pimienta.pos.hardware.PosPrinterRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PosScannerRegistry
import io.github.alexistrejo.pimienta.pos.hardware.UsbPrintTransport
import androidx.core.content.ContextCompat

// Identifies the visible panel used by portrait tablets during a draft sale.
internal enum class PortraitPanel { CATALOG, CART }

// Hosts the offline POS and restores the persisted local state on launch.
class MainActivity : ComponentActivity() {
    private val hidScanner = HidKeyboardBarcodeScanner()
    private val fakeScanner = FakeBarcodeScanner()
    private val barcodeScanner = MultiplexBarcodeScanner(listOf(hidScanner, fakeScanner))
    private var usbReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PosScannerRegistry.hid = hidScanner
        PosScannerRegistry.fake = fakeScanner
        PosScannerRegistry.primary = barcodeScanner
        registerUsbReceiver()

        val preferences = getSharedPreferences("pos-demo", MODE_PRIVATE)

        setContent {
            var dark by remember {
                mutableStateOf(preferences.getBoolean("dark-theme", true))
            }
            PosTheme(dark) {
                PosApp(barcodeScanner, dark) { enabled ->
                    dark = enabled
                    preferences.edit { putBoolean("dark-theme", enabled) }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (hidScanner.onKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        usbReceiver?.let { unregisterReceiver(it) }
        usbReceiver = null
        super.onDestroy()
    }

    // Refreshes printer status and drains queued tickets after USB permission or reconnect.
    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbPrintTransport.ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    UsbPrintTransport.ACTION_USB_PERMISSION -> {
                        PosPrinterRegistry.notifyChanged()
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            PrintWorker.enqueue(context)
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        PosPrinterRegistry.notifyChanged()
                        UsbPrintTransport.requestPermissionIfNeeded(context)
                        PrintWorker.enqueue(context)
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> PosPrinterRegistry.notifyChanged()
                }
            }
        }
        ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}

// Selects the active data space and keeps sandbox isolated from backend services.
@Composable
private fun PosApp(scanner: BarcodeScanner, dark: Boolean, onTheme: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as PosApplication
    // Recreated after mode switches so queries hit the newly selected database.
    var repository by remember { mutableStateOf(PosRepository(app.databaseProvider)) }
    val mode = repository.mode()
    var users by remember { mutableStateOf<List<LocalUserEntity>>(emptyList()) }
    var products by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var policy by remember { mutableStateOf<PosPolicyEntity?>(null) }
    var shift by remember { mutableStateOf<ShiftEntity?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var enrolling by remember { mutableStateOf(false) }
    var enrollError by remember { mutableStateOf<String?>(null) }
    var syncState by remember { mutableStateOf<SyncStateEntity?>(null) }
    var initialized by remember { mutableStateOf(false) }
    var enrolled by remember { mutableStateOf(false) }
    var loadGeneration by remember { mutableStateOf(0) }
    var productionCatalogSyncAttempted by remember { mutableStateOf(false) }

    fun reload() {
        val generation = loadGeneration + 1
        loadGeneration = generation
        val repo = repository
        scope.launch {
            val state = withContext(Dispatchers.IO) {
                app.awaitActiveDatabaseReady()
                // Only force re-enrollment when explicitly revoked or no credentials remain.
                if (mode == RuntimeMode.PRODUCTION) {
                    val deviceCredentials = DeviceCredentials(context)
                    val sync = repo.syncState()
                    val hasUrl = !sync?.baseUrl.isNullOrBlank()
                    val hasAccess = deviceCredentials.access() != null
                    val hasRefresh = deviceCredentials.refresh() != null
                    if (sync?.status == "REQUIRES_REENROLLMENT") {
                        // Already flagged by sync worker; keep enrollment screen without wiping again.
                    } else if (hasUrl && !hasAccess && !hasRefresh) {
                        ProvisioningRepository(context, app.databaseProvider).resetForReenrollment(
                            sync?.lastError ?: DeviceSessionPolicy.orphanAccessTokenMessage(),
                        )
                    } else if (hasUrl && !hasAccess && hasRefresh) {
                        SyncWorker.enqueue(context)
                    }
                }
                Quadruple(repo.syncState(), repo.users(), repo.products(), repo.activeShift())
            }
            if (generation != loadGeneration) return@launch
            syncState = state.first
            users = state.second
            products = state.third
            shift = state.fourth
            initialized = true
        }
    }

    // Enrollment check must stay off the composition/main thread (Room rule).
    LaunchedEffect(Unit) {
        if (!BuildConfig.DEBUG) {
            enrolled = withContext(Dispatchers.IO) { TrainingModePolicy.isEnrolled(app.databaseProvider) }
        }
    }
    val requiresPinForSwitch = TrainingModePolicy.requiresPinForModeSwitch(isEnrolled = enrolled)

    fun switchMode(target: RuntimeMode, pin: String?) {
        scope.launch {
            if (shift != null) {
                notice = "Cierra el turno antes de cambiar de modo"
                return@launch
            }
            if (requiresPinForSwitch) {
                val productionRepository = PosRepository(app.databaseProvider, RuntimeMode.PRODUCTION)
                val activeRepo = PosRepository(app.databaseProvider)
                val manager = withContext(Dispatchers.IO) {
                    productionRepository.users().firstOrNull { it.active && it.isManagerOrAdmin }
                        ?: activeRepo.users().firstOrNull { it.active && it.isManagerOrAdmin }
                }
                val valid = withContext(Dispatchers.IO) {
                    manager != null && (productionRepository.authenticate(manager.id, pin ?: "") || activeRepo.authenticate(manager.id, pin ?: ""))
                }
                if (!valid) {
                    notice = "PIN de Manager/Superadmin invalido"
                    return@launch
                }
            }
            initialized = false
            withContext(Dispatchers.IO) {
                if (target == RuntimeMode.SANDBOX) app.enterTrainingMode() else app.exitTrainingMode()
            }
            repository = PosRepository(app.databaseProvider)
            shift = null
            notice = null
            reload()
        }
    }

    fun resetTrainingDemo() {
        scope.launch {
            if (shift != null) {
                notice = "Cierra el turno antes de reiniciar los datos demo"
                return@launch
            }
            initialized = false
            withContext(Dispatchers.IO) { app.resetTrainingPlayground() }
            shift = null
            reload()
        }
    }

    LaunchedEffect(Unit) { reload() }

    // Room flows keep catalog and policy state live after every committed sync transaction.
    LaunchedEffect(repository) {
        launch { repository.observeProducts().collect { products = it } }
        launch { repository.observeUsers().collect { users = it } }
        launch { repository.observePolicy().collect { policy = it } }
        launch { repository.observeSyncState().collect { syncState = it } }
    }

    // When production is enrolled but catalog/operators are empty, pull bootstrap once.
    LaunchedEffect(initialized, mode, syncState?.baseUrl, syncState?.status) {
        if (!initialized) return@LaunchedEffect
        if (mode != RuntimeMode.PRODUCTION) return@LaunchedEffect
        if (syncState?.baseUrl.isNullOrBlank() || syncState?.status == "REQUIRES_REENROLLMENT") return@LaunchedEffect
        if (productionCatalogSyncAttempted) return@LaunchedEffect
        if (users.isNotEmpty() && products.isNotEmpty()) return@LaunchedEffect
        productionCatalogSyncAttempted = true
        notice = "Sincronizando con el servidor…"
        notice = withContext(Dispatchers.IO) { runForegroundSync(context) }
        reload()
    }

    val showEnrollment = mode == RuntimeMode.PRODUCTION &&
        (syncState?.baseUrl == null || syncState?.status == "REQUIRES_REENROLLMENT")

    LaunchedEffect(scanner) {
        (scanner as? MultiplexBarcodeScanner)?.attach(this)
    }
    // HID wedge must not swallow on-screen digits while the enrollment form is visible.
    LaunchedEffect(scanner, initialized, showEnrollment) {
        if (initialized && !showEnrollment) scanner.start() else scanner.stop()
    }

    // Insets once at the root so Sale (and siblings) do not add a second black status-bar gap under the banner.
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        if (shift == null) {
            RuntimeModeBanner(
                mode = mode,
                dark = dark,
                onTheme = onTheme,
                requiresPinForSwitch = requiresPinForSwitch,
                onSwitchRequested = ::switchMode,
                onResetDemo = if (BuildConfig.DEBUG && mode == RuntimeMode.SANDBOX) ::resetTrainingDemo else null,
                onForceSync = {
                    scope.launch {
                        notice = "Sincronizando con el servidor…"
                        notice = withContext(Dispatchers.IO) { runForegroundSync(context) }
                        reload()
                    }
                },
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (!initialized) {
                Loading(loadingMessage(mode, null), ::reload)
            } else {
                when {
                    showEnrollment ->
                        EnrollmentScreen(
                            busy = enrolling,
                            error = enrollError ?: syncState?.lastError,
                        ) { code, name ->
                        scope.launch {
                            enrolling = true
                            enrollError = null
                            try {
                                withContext(Dispatchers.IO) { ProvisioningRepository(context, app.databaseProvider).enroll(PRODUCTION_API_URL, code, name) }
                                productionCatalogSyncAttempted = false
                                SyncWorker.enqueue(context)
                                reload()
                            } catch (e: Exception) {
                                // Enroll may have saved tokens before bootstrap failed; leave enrollment UI so sync can retry.
                                val alreadyConfigured = withContext(Dispatchers.IO) {
                                    ProvisioningRepository(context, app.databaseProvider).baseUrl() != null &&
                                        DeviceCredentials(context).access() != null
                                }
                                val userMessage = PosApiUserMessages.from(e)
                                if (alreadyConfigured) {
                                    notice = userMessage
                                    SyncWorker.enqueue(context)
                                    reload()
                                } else {
                                    enrollError = userMessage
                                }
                            } finally { enrolling = false }
                        }
                    }
                    users.isEmpty() || products.isEmpty() -> {
                        val waitingMessage = when {
                            !notice.isNullOrBlank() -> notice
                            !syncState?.lastError.isNullOrBlank() -> syncState?.lastError
                            users.isEmpty() && products.isNotEmpty() ->
                                "Catálogo recibido (${products.size} producto(s)), pero falta: operadores POS con PIN en esta sede."
                            users.isNotEmpty() && products.isEmpty() ->
                                "Operadores recibidos (${users.size}), pero falta: productos en el catálogo POS de la sede (o hay que resincronizar bootstrap)."
                            else -> loadingMessage(mode, null)
                        }
                        Loading(
                            message = waitingMessage,
                            reload = {
                                scope.launch {
                                    productionCatalogSyncAttempted = true
                                    notice = "Sincronizando con el servidor…"
                                    notice = withContext(Dispatchers.IO) { runForegroundSync(context) }
                                    reload()
                                }
                            },
                            onResetEnrollment = if (mode == RuntimeMode.PRODUCTION) {
                                {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            ProvisioningRepository(context, app.databaseProvider)
                                                .resetForReenrollment("Reenrolamiento solicitado desde la tablet.")
                                        }
                                        enrollError = null
                                        notice = null
                                        productionCatalogSyncAttempted = false
                                        reload()
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                    shift == null -> Access(users, repository, notice, mode, { shift = it }, { notice = it })
                    else -> {
                        val activeShift = shift
                        if (activeShift != null) {
                            Sale(
                                repository = repository,
                                shift = activeShift,
                                cashier = users.firstOrNull { it.id == activeShift.cashierId }?.displayName ?: "Cajero",
                                users = users,
                                products = products,
                                policy = policy,
                                syncState = syncState,
                                dark = dark,
                                onTheme = onTheme,
                                onShiftClosed = { shift = null },
                                scanner = scanner,
                            )
                        } else {
                            Access(users, repository, notice, mode, { shift = it }, { notice = it })
                        }
                    }
                }
            }
        }
    }
}

// Small immutable tuple used to load the active database state together.
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun loadingMessage(mode: RuntimeMode, notice: String?): String = notice ?: when (mode) {
    RuntimeMode.SANDBOX -> "Preparando entorno de capacitación…"
    RuntimeMode.PRODUCTION -> "Cargando datos de venta…"
}

// Waits for the training template import without blocking the Compose UI thread.
@Composable
private fun Loading(
    message: String?,
    reload: () -> Unit,
    onResetEnrollment: (() -> Unit)? = null,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
            )
            Text("Pimienta POS", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(
                message ?: "Preparando datos locales…",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            PosButton("Sincronizar con el servidor", reload, primary = true)
            if (onResetEnrollment != null) {
                Spacer(Modifier.height(12.dp))
                PosButton("Volver a enrolar dispositivo", onResetEnrollment, primary = false)
            }
        }
    }
}

// Determines which users can be assigned a shift based on the authenticated authorizer's role.
internal fun allowedShiftAssignees(authorizer: LocalUserEntity, users: List<LocalUserEntity>): List<LocalUserEntity> {
    val activeUsers = users.filter { it.active }
    return if (authorizer.isSuperAdmin) {
        // Superadmin can assign shifts to anyone (themselves, cashiers, managers, other superadmins).
        activeUsers
    } else {
        // Managers can only assign shifts to themselves or cashiers (not other managers or superadmins).
        activeUsers.filter { it.id == authorizer.id || !it.isManagerOrAdmin }
    }
}

// Represents the stages of the multi-step shift opening flow.
internal enum class AccessStage {
    AUTHENTICATE_AUTHORIZER,
    SELECT_ASSIGNEE_AND_CASH,
}

// Displays a visual two-step progress indicator at the top of the shift opening workflow.
@Composable
private fun ShiftOpeningStepHeader(currentStage: AccessStage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val isStep1 = currentStage == AccessStage.AUTHENTICATE_AUTHORIZER

        // Step 1 Badge
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = if (isStep1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = "PASO 1 DE 2",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isStep1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "1. Autorización PIN",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isStep1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Step 2 Badge
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = if (!isStep1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = "PASO 2 DE 2",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!isStep1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "2. Cajero y Fondo",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (!isStep1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Handles authorization and shift assignment workflow before opening a local shift.
@Composable
internal fun Access(
    users: List<LocalUserEntity>,
    repository: PosRepository,
    notice: String?,
    mode: RuntimeMode,
    opened: (ShiftEntity) -> Unit,
    message: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Filters active managers and superadmins eligible to authorize shift opening.
    val authorizers = remember(users) { users.filter { it.active && it.isManagerOrAdmin } }
    var stage by remember { mutableStateOf(AccessStage.AUTHENTICATE_AUTHORIZER) }
    var selectedAuthorizer by remember(authorizers) { mutableStateOf(authorizers.firstOrNull()) }
    var authenticatedAuthorizer by remember { mutableStateOf<LocalUserEntity?>(null) }
    var pin by remember { mutableStateOf("") }

    var selectedAssignee by remember { mutableStateOf<LocalUserEntity?>(null) }
    var opening by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

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
                // Header with step progress bar
                ShiftOpeningStepHeader(stage)

                if (stage == AccessStage.AUTHENTICATE_AUTHORIZER) {
                    // Step 1: Authorizer election and PIN verification
                    Text("Abrir turno", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Paso 1: Identificación del autorizador",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Selecciona tu perfil (Gerente o Superadmin) e ingresa tu PIN de seguridad para autorizar la apertura del turno.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    // Operational warning regarding sale mode constraints during active shifts.
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (mode == RuntimeMode.SANDBOX) {
                                "Modo capacitación: Una vez abierto el turno, el sistema permanecerá en modo de venta hasta completar el corte de caja."
                            } else {
                                "Aviso importante: Al abrir turno, la tablet queda en modo de venta exclusivo y no se podrá salir ni cambiar de modo hasta realizar el corte de caja."
                            },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }

                    if (authorizers.isEmpty()) {
                        Text(
                            "No hay usuarios Manager o Superadmin activos registrados en la tablet.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text("1. Selecciona el perfil del autorizador:", style = MaterialTheme.typography.titleSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(authorizers, key = { it.id }) { profile ->
                                PosButton(
                                    profile.displayTitle(mode == RuntimeMode.SANDBOX),
                                    { selectedAuthorizer = profile },
                                    selected = selectedAuthorizer?.id == profile.id,
                                )
                            }
                        }
                    }

                    Text("2. Ingresa tu PIN de autorización:", style = MaterialTheme.typography.titleSmall)
                    Numpad(pin, { pin = it }, masked = true)

                    val activeError = localError ?: notice
                    activeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    PosButton(
                        label = if (busy) "Verificando PIN…" else "Continuar a asignar cajero y fondo →",
                        click = {
                            val authorizer = selectedAuthorizer
                            if (authorizer == null) {
                                localError = "Selecciona un perfil de Manager o Superadmin."
                                return@PosButton
                            }
                            scope.launch {
                                busy = true
                                localError = null
                                val valid = withContext(Dispatchers.IO) {
                                    repository.authenticate(authorizer.id, pin)
                                }
                                busy = false
                                if (!valid) {
                                    localError = "El PIN no corresponde al perfil seleccionado."
                                } else {
                                    authenticatedAuthorizer = authorizer
                                    val allowed = allowedShiftAssignees(authorizer, users)
                                    selectedAssignee = allowed.firstOrNull { it.id == authorizer.id } ?: allowed.firstOrNull()
                                    stage = AccessStage.SELECT_ASSIGNEE_AND_CASH
                                    localError = null
                                }
                            }
                        },
                        enabled = !busy && selectedAuthorizer != null,
                        primary = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // Step 2: Assignee election and initial cash float
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Abrir turno", style = MaterialTheme.typography.headlineSmall)
                        PosButton(
                            label = "← Cambiar autorizador",
                            click = {
                                stage = AccessStage.AUTHENTICATE_AUTHORIZER
                                authenticatedAuthorizer = null
                                pin = ""
                                localError = null
                            },
                            primary = false,
                        )
                    }

                    // Authenticated Manager banner badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "AUTORIZACIÓN CONFIRMADA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                "Autorizado por: ${authenticatedAuthorizer?.displayName ?: ""} (${authenticatedAuthorizer?.role ?: ""})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }

                    Text(
                        "Paso 2: Asignación de cajero y fondo inicial",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Elige la persona que operará la caja durante este turno e ingresa el efectivo inicial con el que iniciará el cajón de dinero.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    val auth = authenticatedAuthorizer
                    val allowedAssignees = remember(auth, users) {
                        if (auth != null) allowedShiftAssignees(auth, users) else emptyList()
                    }

                    Text("1. ¿Quién operará la caja en este turno?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (auth?.isSuperAdmin == true) {
                            "Como Superadmin, puedes asignar este turno a cualquier perfil registrado."
                        } else {
                            "Como Gerente, puedes asignar el turno a ti mismo o a un cajero. (No se permite asignar turno a otros gerentes)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allowedAssignees, key = { it.id }) { profile ->
                            PosButton(
                                profile.displayTitle(mode == RuntimeMode.SANDBOX),
                                { selectedAssignee = profile },
                                selected = selectedAssignee?.id == profile.id,
                            )
                        }
                    }

                    Text("2. Fondo inicial en efectivo (Cajón de dinero):", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Ingresa la cantidad exacta de efectivo entregada para iniciar caja.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(Money.format(Money.fromInput(opening) ?: 0), style = MaterialTheme.typography.headlineMedium)
                    Numpad(opening, { opening = it })

                    val activeError = localError ?: notice
                    activeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    PosButton(
                        label = if (busy) "Abriendo turno…" else "Confirmar y abrir turno",
                        click = {
                            val cash = Money.fromInput(opening) ?: -1
                            val assignee = selectedAssignee
                            if (cash < 0) {
                                localError = "Ingresa un fondo inicial válido."
                                return@PosButton
                            }
                            if (assignee == null) {
                                localError = "Selecciona la persona a la que se asignará el turno."
                                return@PosButton
                            }
                            scope.launch {
                                busy = true
                                localError = null
                                val result = withContext(Dispatchers.IO) {
                                    repository.openShift(assignee.id, cash)?.let { Result.success(it) }
                                        ?: Result.failure(IllegalStateException("No se pudo abrir el turno local."))
                                }
                                busy = false
                                result.onSuccess { shift ->
                                    SyncWorker.enqueue(context)
                                    opened(shift)
                                }.onFailure { ex ->
                                    val err = ex.message ?: "No se pudo abrir el turno."
                                    localError = err
                                    message(err)
                                }
                            }
                        },
                        enabled = !busy && selectedAssignee != null,
                        primary = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

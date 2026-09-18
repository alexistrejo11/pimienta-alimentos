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

    LaunchedEffect(scanner) {
        (scanner as? MultiplexBarcodeScanner)?.attach(this)
        scanner.start()
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
                    mode == RuntimeMode.PRODUCTION &&
                        (syncState?.baseUrl == null || syncState?.status == "REQUIRES_REENROLLMENT") ->
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
                    else -> Sale(
                        repository = repository,
                        shift = shift!!,
                        cashier = users.firstOrNull { it.id == shift!!.cashierId }?.displayName ?: "Cajero",
                        users = users,
                        products = products,
                        policy = policy,
                        syncState = syncState,
                        dark = dark,
                        onTheme = onTheme,
                        onShiftClosed = { shift = null },
                        scanner = scanner,
                    )
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

// Verifies a local PIN and collects the opening cash with the integrated numpad.
@Composable
private fun Access(
    users: List<LocalUserEntity>,
    repository: PosRepository,
    notice: String?,
    mode: RuntimeMode,
    opened: (ShiftEntity) -> Unit,
    message: (String) -> Unit,
) {
    val context = LocalContext.current
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
                
                // Red text warning before opening shift about not being able to exit sale mode.
                Text(
                    text = if (mode == RuntimeMode.SANDBOX) {
                        "Modo capacitación: Una vez abierto el turno, el sistema permanecerá en modo de venta hasta completar el corte de caja."
                    } else {
                        "Una vez abierto el turno, no se podrá salir del punto de venta ni cambiar de modo hasta completar el corte de caja."
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users, key = { it.id }) { profile ->
                        PosButton(profile.displayTitle(mode == RuntimeMode.SANDBOX), { user = profile }, selected = user.id == profile.id)
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
                            result.onSuccess { shift ->
                                SyncWorker.enqueue(context)
                                opened(shift)
                            }.onFailure { message(it.message ?: "No se pudo abrir el turno.") }
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

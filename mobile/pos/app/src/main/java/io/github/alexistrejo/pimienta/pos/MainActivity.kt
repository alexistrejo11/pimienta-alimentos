package io.github.alexistrejo.pimienta.pos

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import io.github.alexistrejo.pimienta.pos.app.AndroidSessionEnvironment
import io.github.alexistrejo.pimienta.pos.app.PosApplication
import io.github.alexistrejo.pimienta.pos.app.SessionCoordinator
import io.github.alexistrejo.pimienta.pos.app.SessionDestination
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
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
    private var bluetoothReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PosScannerRegistry.hid = hidScanner
        PosScannerRegistry.fake = fakeScanner
        PosScannerRegistry.primary = barcodeScanner
        registerUsbReceiver()
        registerBluetoothReceiver()
        handleUsbAttachIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUsbAttachIntent(intent)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (hidScanner.onKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        usbReceiver?.let { unregisterReceiver(it) }
        usbReceiver = null
        bluetoothReceiver?.let { unregisterReceiver(it) }
        bluetoothReceiver = null
        super.onDestroy()
    }

    // Keeps the current activity when a printer attaches instead of stacking a new Sale screen.
    private fun handleUsbAttachIntent(intent: Intent?) {
        if (intent?.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return
        onUsbPrinterEvent(this, intent)
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
                onUsbPrinterEvent(context, intent)
            }
        }
        ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    // Drains the print queue when the saved thermal printer's ACL comes back.
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onBluetoothPrinterEvent(context, intent)
            }
        }
        ContextCompat.registerReceiver(this, bluetoothReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    companion object {
        // Ignores hub/HID attaches and coalesces printer permission prompts.
        private fun onUsbPrinterEvent(context: Context, intent: Intent) {
            val device = UsbPrintTransport.deviceFrom(intent)
            when (intent.action) {
                UsbPrintTransport.ACTION_USB_PERMISSION -> {
                    if (device != null) UsbPrintTransport.markPermissionResolved(device)
                    PosPrinterRegistry.notifyChanged()
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        PrintWorker.enqueue(context)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    if (device != null && !UsbPrintTransport.isPrinterCandidate(device)) return
                    PosPrinterRegistry.notifyChanged()
                    UsbPrintTransport.requestPermissionIfNeeded(context, device)
                    PrintWorker.enqueue(context)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    if (device != null) UsbPrintTransport.markPermissionResolved(device)
                    PosPrinterRegistry.notifyChanged()
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun onBluetoothPrinterEvent(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                        PosPrinterRegistry.onBluetoothRadioOff()
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = bluetoothDeviceFrom(intent) ?: return
                    val mac = try {
                        device.address
                    } catch (_: SecurityException) {
                        return
                    }
                    val connected = intent.action == BluetoothDevice.ACTION_ACL_CONNECTED
                    PosPrinterRegistry.onBluetoothAcl(context, mac, connected)
                    if (connected) PrintWorker.enqueueAfterLink(context)
                }
            }
        }

        private fun bluetoothDeviceFrom(intent: Intent): BluetoothDevice? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
        }
    }
}

// Renders the root screen for the current session state and forwards user events to the coordinator.
@Composable
private fun PosApp(scanner: BarcodeScanner, dark: Boolean, onTheme: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as PosApplication
    val coordinator = remember { SessionCoordinator(scope, AndroidSessionEnvironment(app)) }
    val state by coordinator.state.collectAsState()
    // Recreated after mode switches so queries hit the newly selected database.
    val repository = remember(state.mode) { PosRepository(app.databaseProvider, state.mode) }
    var standaloneManagerId by rememberSaveable { mutableStateOf<String?>(null) }
    val standaloneManager = remember(standaloneManagerId, state.users) { state.users.firstOrNull { it.id == standaloneManagerId } }
    var managerAccessOpen by rememberSaveable { mutableStateOf(false) }
    var devicesOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(coordinator) { coordinator.start() }

    if (managerAccessOpen) {
        ManagerAccess(
            users = state.users,
            repository = repository,
            onDismiss = { managerAccessOpen = false },
            onAuthorized = { authed ->
                managerAccessOpen = false
                standaloneManagerId = authed.id
            },
        )
    }

    LaunchedEffect(scanner) {
        (scanner as? MultiplexBarcodeScanner)?.attach(this)
    }
    // HID wedge must not swallow on-screen digits while the enrollment form is visible.
    LaunchedEffect(scanner, state.initialized, state.showEnrollment) {
        if (state.initialized && !state.showEnrollment) scanner.start() else scanner.stop()
    }

    // Insets once at the root so Sale (and siblings) do not add a second black status-bar gap under the banner.
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        if (state.shift == null) {
            RuntimeModeBanner(
                mode = state.mode,
                dark = dark,
                onTheme = onTheme,
                requiresPinForSwitch = state.requiresPinForSwitch,
                authorizers = state.switchAuthorizers,
                onSwitchRequested = coordinator::switchMode,
                onResetDemo = if (state.canResetTrainingDemo) {
                    { coordinator.resetTrainingDemo() }
                } else {
                    null
                },
                availableUpdateVersionName = state.availableUpdateVersionName,
                onForceSync = { coordinator.syncNow() },
                onOpenManager = { managerAccessOpen = true },
                onOpenDevices = { devicesOpen = true },
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val activeStandaloneManager = standaloneManager
            if (activeStandaloneManager != null) {
                ManagerPanel(
                    shift = state.shift,
                    manager = activeStandaloneManager,
                    users = state.users,
                    products = state.products,
                    pendingEvents = state.pendingEventsCount,
                    repository = repository,
                    dark = dark,
                    onTheme = onTheme,
                    onReturnToSale = { standaloneManagerId = null },
                    onShiftClosed = {
                        coordinator.onShiftClosed()
                        standaloneManagerId = null
                    },
                )
            } else if (devicesOpen) {
                DevicesScreen(
                    repository = repository,
                    onReturn = { devicesOpen = false },
                    returnLabel = if (state.shift != null) "Volver a caja" else "Volver",
                )
            } else {
                when (val destination = state.destination) {
                    is SessionDestination.Loading -> Loading(destination.message, { coordinator.reload() })
                    SessionDestination.Enrollment ->
                        EnrollmentScreen(
                            busy = state.enrolling,
                            error = state.enrollmentErrorMessage,
                        ) { code, name -> coordinator.enroll(code, name) }
                    is SessionDestination.WaitingForCatalog -> Loading(
                        message = destination.message,
                        reload = { coordinator.syncNow(markCatalogSyncAttempted = true) },
                        onResetEnrollment = if (destination.canResetEnrollment) {
                            { coordinator.requestReenrollment() }
                        } else {
                            null
                        },
                    )
                    SessionDestination.Access -> Access(
                        users = state.users,
                        repository = repository,
                        notice = state.notice,
                        mode = state.mode,
                        opened = coordinator::onShiftOpened,
                        message = coordinator::showNotice,
                    )
                    is SessionDestination.Sale -> Sale(
                        repository = repository,
                        shift = destination.shift,
                        cashier = state.users.firstOrNull { it.id == destination.shift.cashierId }?.displayName ?: "Cajero",
                        users = state.users,
                        products = state.products,
                        policy = state.policy,
                        syncState = state.syncState,
                        dark = dark,
                        onTheme = onTheme,
                        onShiftClosed = { coordinator.onShiftClosed() },
                        scanner = scanner,
                    )
                }
            }
        }
    }
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
    return if (authorizer.isAdmin) {
        // Admin can assign the shift to any active profile.
        activeUsers
    } else {
        // Managers can only assign shifts to themselves or cashiers (not other managers or admins).
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

    // Filters active managers and admins eligible to authorize shift opening.
    val authorizers = remember(users) { users.filter { it.active && it.isManagerOrAdmin } }
    var stage by rememberSaveable { mutableStateOf(AccessStage.AUTHENTICATE_AUTHORIZER) }
    var selectedAuthorizerId by rememberSaveable { mutableStateOf((authorizers.firstOrNull())?.id) }
    val selectedAuthorizer = remember(selectedAuthorizerId, authorizers) { authorizers.firstOrNull { it.id == selectedAuthorizerId } ?: authorizers.firstOrNull() }
    var authenticatedAuthorizerId by rememberSaveable { mutableStateOf<String?>(null) }
    val authenticatedAuthorizer = remember(authenticatedAuthorizerId, users) { users.firstOrNull { it.id == authenticatedAuthorizerId } }
    var pin by rememberSaveable { mutableStateOf("") }

    var selectedAssigneeId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedAssignee = remember(selectedAssigneeId, users) { users.firstOrNull { it.id == selectedAssigneeId } }
    var opening by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                // Header with step progress bar
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (stage == AccessStage.AUTHENTICATE_AUTHORIZER) {
                        Text("Abrir turno", style = MaterialTheme.typography.headlineSmall)
                    } else {
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
                                    authenticatedAuthorizerId = null
                                    pin = ""
                                    localError = null
                                },
                                primary = false,
                            )
                        }
                    }
                    ShiftOpeningStepHeader(stage)
                }

                if (stage == AccessStage.AUTHENTICATE_AUTHORIZER) {
                    // Step 1: Authorizer election and PIN verification
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Paso 1: Identificación del autorizador",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Selecciona tu perfil (Gerente o Administrador) e ingresa tu PIN de seguridad para autorizar la apertura del turno.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
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
                    }

                    if (authorizers.isEmpty()) {
                        Text(
                            "No hay usuarios Gerente o Administrador activos registrados en la tablet.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("1. Selecciona el perfil del autorizador:", style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(authorizers, key = { it.id }) { profile ->
                                    PosButton(
                                        profile.displayTitle(mode == RuntimeMode.SANDBOX),
                                        { selectedAuthorizerId = profile.id },
                                        selected = selectedAuthorizer?.id == profile.id,
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2. Ingresa tu PIN de autorización:", style = MaterialTheme.typography.titleSmall)
                        Numpad(pin, { pin = it }, masked = true)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val activeErr1 = localError ?: notice?.takeIf { !it.startsWith("✓") }
                        activeErr1?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                        PosButton(
                            label = if (busy) "Verificando PIN…" else "Continuar a asignar cajero y fondo →",
                            click = {
                                val authorizer = selectedAuthorizer
                                if (authorizer == null) {
                                    localError = "Selecciona un perfil de Gerente o Administrador."
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
                                        authenticatedAuthorizerId = authorizer.id
                                        val allowed = allowedShiftAssignees(authorizer, users)
                                        selectedAssigneeId = (allowed.firstOrNull { it.id == authorizer.id } ?: allowed.firstOrNull())?.id
                                        stage = AccessStage.SELECT_ASSIGNEE_AND_CASH
                                        localError = null
                                    }
                                }
                            },
                            enabled = !busy && selectedAuthorizer != null,
                            primary = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    // Step 2: Assignee election and initial cash float
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    "Autorizado por: ${authenticatedAuthorizer?.displayName ?: ""} (${authenticatedAuthorizer?.spanishRoleLabel ?: ""})",
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
                    }

                    val auth = authenticatedAuthorizer
                    val allowedAssignees = remember(auth, users) {
                        if (auth != null) allowedShiftAssignees(auth, users) else emptyList()
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. ¿Quién operará la caja en este turno?", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (auth?.isAdmin == true) {
                                "Como Administrador, puedes asignar este turno a cualquier perfil registrado."
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
                                    { selectedAssigneeId = profile.id },
                                    selected = selectedAssignee?.id == profile.id,
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2. Fondo inicial en efectivo (Cajón de dinero):", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Ingresa la cantidad exacta de efectivo entregada para iniciar caja.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Numpad(opening, { opening = it }, decimal = true)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val activeErr2 = localError ?: notice?.takeIf { !it.startsWith("✓") }
                        activeErr2?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                        PosButton(
                            label = if (busy) "Abriendo turno…" else "Confirmar y abrir turno",
                            click = {
                                if (busy) return@PosButton
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
                                busy = true
                                scope.launch {
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
}

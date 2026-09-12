package io.github.alexistrejo.pimienta.pos

import android.os.Bundle
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexistrejo.pimienta.pos.app.PosApplication
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.compose.ui.platform.LocalContext
import io.github.alexistrejo.pimienta.pos.data.sync.ProvisioningRepository
import io.github.alexistrejo.pimienta.pos.data.sync.PRODUCTION_API_URL
import io.github.alexistrejo.pimienta.pos.hardware.FakeBarcodeScanner
import io.github.alexistrejo.pimienta.pos.hardware.BarcodeScanner

// Identifies the visible panel used by portrait tablets during a draft sale.
internal enum class PortraitPanel { CATALOG, CART }

// Hosts the offline POS and restores the persisted local state on launch.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = getSharedPreferences("pos-demo", MODE_PRIVATE)
        val repository = PosRepository((application as PosApplication).databaseProvider)

        setContent {
            var dark by remember {
                mutableStateOf(if (BuildConfig.DEBUG) preferences.getBoolean("dark-theme", true) else true)
            }
            PosTheme(dark) {
                PosApp(repository, dark) { enabled ->
                    dark = enabled
                    preferences.edit { putBoolean("dark-theme", enabled) }
                }
            }
        }
    }
}

// Selects the active data space and keeps sandbox isolated from backend services.
@Composable
private fun PosApp(repository: PosRepository, dark: Boolean, onTheme: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as PosApplication
    val mode = repository.mode()
    val scanner: BarcodeScanner? = remember { if (BuildConfig.DEBUG) FakeBarcodeScanner() else null }
    var users by remember { mutableStateOf<List<LocalUserEntity>>(emptyList()) }
    var products by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var shift by remember { mutableStateOf<ShiftEntity?>(null) }
    var managerReadOnly by remember { mutableStateOf<LocalUserEntity?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var enrolling by remember { mutableStateOf(false) }
    var enrollError by remember { mutableStateOf<String?>(null) }
    var syncState by remember { mutableStateOf<SyncStateEntity?>(null) }
    var initialized by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            val state = withContext(Dispatchers.IO) {
                Quadruple(repository.syncState(), repository.users(), repository.products(), repository.activeShift())
            }
            syncState = state.first
            users = state.second
            products = state.third
            shift = state.fourth
            initialized = true
        }
    }

    fun switchMode(target: RuntimeMode, pin: String) {
        scope.launch {
            val managerRepository = if (users.isNotEmpty()) repository else PosRepository(app.databaseProvider, RuntimeMode.SANDBOX)
            val manager = withContext(Dispatchers.IO) {
                managerRepository.users().firstOrNull { it.active && (it.role.equals("MANAGER", true) || it.role.equals("SUPERADMIN", true)) }
            }
            val valid = withContext(Dispatchers.IO) { manager != null && managerRepository.authenticate(manager.id, pin) }
            if (!valid) { notice = "PIN de Manager/Superadmin invalido"; return@launch }
            if (shift != null) { notice = "Cierra el turno antes de cambiar de modo"; return@launch }
            app.switchMode(target)
            (context as? ComponentActivity)?.recreate()
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column {
        RuntimeModeBanner(mode, ::switchMode)
        if (!initialized) {
            Loading("Cargando base de datos...", ::reload)
        } else {
            when {
                mode == RuntimeMode.PRODUCTION && syncState?.baseUrl == null -> EnrollmentScreen( busy = enrolling, error = enrollError ) { code, name ->
                    scope.launch {
                        enrolling = true
                        enrollError = null
                        try {
                            withContext(Dispatchers.IO) { ProvisioningRepository(context, app.databaseProvider).enroll(PRODUCTION_API_URL, code, name) }
                            reload()
                        } catch (e: Exception) {
                            enrollError = e.message ?: "No fue posible enrolar el dispositivo"
                        } finally { enrolling = false }
                    }
                }
                users.isEmpty() || products.isEmpty() -> Loading(notice, ::reload)
                shift == null && managerReadOnly != null -> ManagerReadOnlyPanel(managerReadOnly!!, repository) { managerReadOnly = null }
                shift == null -> Access(users, repository, notice, { shift = it }, { notice = it }) { managerReadOnly = it }
                else -> Sale(
                    repository = repository,
                    shift = shift!!,
                    cashier = users.firstOrNull { it.id == shift!!.cashierId }?.displayName ?: "Cajero",
                    users = users,
                    products = products,
                    dark = dark,
                    onTheme = onTheme,
                    onShiftClosed = { shift = null },
                    scanner = scanner,
                )
            }
        }
    }
}

// Small immutable tuple used to load the active database state together.
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// Waits for the debug bootstrap without blocking the Compose UI thread.
@Composable
private fun Loading(notice: String?, reload: () -> Unit) {
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
    openManagerDashboard: (LocalUserEntity) -> Unit,
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
                PosButton(
                    label = "Abrir panel Manager (solo lectura)",
                    click = {
                        scope.launch {
                            val authorized = withContext(Dispatchers.IO) {
                                (user.role == "MANAGER" || user.role == "SUPERADMIN") && repository.authenticate(user.id, pin)
                            }
                            if (authorized) openManagerDashboard(user) else message("El PIN no corresponde a un perfil Manager.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

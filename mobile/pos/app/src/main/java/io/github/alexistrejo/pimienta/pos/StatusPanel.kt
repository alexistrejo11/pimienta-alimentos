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
// App updates, sync, and the shared printer/scanner config used by cashiers.
@Composable
internal fun StatusPanel(
    pendingEvents: Int,
    repository: PosRepository,
    modifier: Modifier,
) {
    var syncMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var device by remember { mutableStateOf<DeviceEntity?>(null) }
    var updateMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var updateBusy by rememberSaveable { mutableStateOf(false) }
    var availableUpdateName by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mode = repository.mode()
    val updater = remember(context) { PosAppUpdater(context) }

    fun applyCheckOutcome(outcome: ReleaseCheckOutcome, announceUpToDate: Boolean) {
        when (outcome) {
            is ReleaseCheckOutcome.UpdateAvailable -> {
                availableUpdateName = outcome.remote.versionName
                updateMessage = "Hay una versión nueva: v${outcome.remote.versionName}."
            }
            is ReleaseCheckOutcome.UpToDate -> {
                availableUpdateName = null
                if (announceUpToDate) {
                    val remoteName = outcome.remote.versionName.ifBlank { BuildConfig.VERSION_NAME }
                    updateMessage = "Estás al día (v$remoteName)."
                }
            }
            is ReleaseCheckOutcome.Failed -> {
                updateMessage = outcome.message
            }
        }
    }

    LaunchedEffect(Unit) {
        device = withContext(Dispatchers.IO) { repository.device() }
        availableUpdateName = updater.cachedUpdateVersionName()
        updateBusy = true
        val outcome = withContext(Dispatchers.IO) { updater.check(force = false) }
        updateBusy = false
        applyCheckOutcome(outcome, announceUpToDate = false)
    }

    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Estado y periféricos", style = MaterialTheme.typography.headlineSmall)
            MetricTile("Eventos pendientes", pendingEvents.toString())
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aplicación", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Versión ${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (mode == RuntimeMode.SANDBOX) "Modo Capacitación" else "Modo Venta",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val currentDevice = device
                    if (currentDevice != null) {
                        Text(
                            "Dispositivo ${currentDevice.name} · ${currentDevice.visibleCode}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Estado ${currentDevice.status}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Mínima requerida ${currentDevice.minAppVersion ?: "—"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Dispositivo aún no enrolado en este modo.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PosButton(
                        if (updateBusy) "Consultando…" else "Buscar actualización",
                        {
                            scope.launch {
                                updateBusy = true
                                updateMessage = null
                                val outcome = withContext(Dispatchers.IO) { updater.check(force = true) }
                                updateBusy = false
                                applyCheckOutcome(outcome, announceUpToDate = true)
                            }
                        },
                        enabled = !updateBusy,
                    )
                    val pendingName = availableUpdateName
                    if (pendingName != null) {
                        PosButton(
                            if (updateBusy) "Descargando…" else "Actualizar a v$pendingName",
                            {
                                scope.launch {
                                    updateBusy = true
                                    updateMessage = "Descargando APK…"
                                    when (val result = withContext(Dispatchers.IO) { updater.downloadAndInstall() }) {
                                        ApkInstallOutcome.Started ->
                                            updateMessage =
                                                "Instalador abierto. Confirma la actualización sin desinstalar la app."
                                        ApkInstallOutcome.NeedsInstallPermission -> {
                                            updateMessage =
                                                "Activa «Instalar apps desconocidas» para Pimienta POS y vuelve a intentar."
                                            runCatching {
                                                context.startActivity(updater.installPermissionSettingsIntent())
                                            }
                                        }
                                        is ApkInstallOutcome.Failed -> updateMessage = result.message
                                    }
                                    updateBusy = false
                                }
                            },
                            enabled = !updateBusy,
                            primary = true,
                        )
                    }
                    updateMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sincronización", style = MaterialTheme.typography.titleMedium)
                    Text("Offline-first · los eventos permanecen en Room hasta sincronizar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PosButton(if (syncing) "Sincronizando…" else "Intentar sincronizar ahora", {
                        if (syncing) return@PosButton
                        if (mode == RuntimeMode.PRODUCTION) {
                            syncMessage = "Sincronizando…"
                            syncing = true
                            scope.launch {
                                val msg = withContext(Dispatchers.IO) { runForegroundSync(context) }
                                syncing = false
                                syncMessage = if (msg == "Sincronización completada.") "✓ Sincronización completada." else msg
                            }
                        } else {
                            syncMessage = "Sandbox no envía eventos al backend."
                        }
                    }, enabled = !syncing)
                    syncMessage?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            DevicesPanel(repository = repository, modifier = Modifier.fillMaxWidth(), showHeading = true)
        }
    }
}

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

// Describes the local Manager workspace navigation.
private enum class ManagerSection(val label: String) {
    DASHBOARD("Resumen del día"),
    Z_CLOSE("Caja y Corte de Caja"),
    HISTORY("Historial"),
    PRODUCTS("Productos"),
    CONFIG("Configuración"),
    STATUS("Estado"),
}

// Shows the local dashboard when no shift is open; operational actions stay unavailable.
// Renders the Manager workspace with a visual dashboard and local Room-backed sections.
@Composable
internal fun ManagerPanel(
    shift: ShiftEntity? = null,
    manager: LocalUserEntity,
    users: List<LocalUserEntity> = emptyList(),
    products: List<ProductEntity>,
    pendingEvents: Int,
    repository: PosRepository,
    onReturnToSale: () -> Unit,
    onShiftClosed: () -> Unit = {}
) {
    var activeShift by remember { mutableStateOf(shift) }
    LaunchedEffect(shift) {
        if (shift != null) activeShift = shift
    }
    val effectiveShift = activeShift ?: shift

    val availableSections = remember(effectiveShift) {
        if (effectiveShift != null) {
            ManagerSection.entries
        } else {
            listOf(ManagerSection.DASHBOARD, ManagerSection.PRODUCTS, ManagerSection.CONFIG, ManagerSection.STATUS)
        }
    }
    var section by rememberSaveable { mutableStateOf(ManagerSection.DASHBOARD) }

    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var webCentralMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(effectiveShift?.id, refreshToken) { summary = withContext(Dispatchers.IO) { repository.dailySummary() } }

    val sectionContent = remember {
        movableContentOf { modifier: Modifier ->
            ManagerSectionContent(
                section = section,
                shift = effectiveShift,
                manager = manager,
                users = users,
                products = products,
                pendingEvents = pendingEvents,
                summary = summary,
                repository = repository,
                refresh = { refreshToken++ },
                onShiftClosed = onShiftClosed,
                modifier = modifier,
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val landscape = maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            ManagerHeader(effectiveShift, manager, onReturnToSale) { webCentralMessage = "La URL de Web Central se configurará con el entorno de la sede; las operaciones locales siguen disponibles." }
            webCentralMessage?.let { Text(it, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (landscape) Row(Modifier.weight(1f).fillMaxWidth()) {
                ManagerSideNav(section, { section = it }, availableSections, Modifier.width(188.dp).fillMaxHeight())
                HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                sectionContent(Modifier.weight(1f))
            } else {
                ManagerCompactNav(section, { section = it }, availableSections)
                sectionContent(Modifier.weight(1f))
            }
        }
    }
}

// Keeps return and Web Central shortcuts visible in every section.
@Composable
private fun ManagerHeader(shift: ShiftEntity?, manager: LocalUserEntity, onReturn: () -> Unit, onOpenWebCentral: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosButton(
            label = if (shift != null) "Volver a caja" else "Volver",
            click = onReturn,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        )
        Column(Modifier.weight(1f)) {
            Text("Panel de control", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (shift != null) "Turno ${shift.id.take(4).uppercase()} · ${manager.displayName}"
                else "Sin turno activo · ${manager.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PosButton(
            label = "Web Central",
            click = onOpenWebCentral,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// Provides persistent landscape navigation.
@Composable
private fun ManagerSideNav(selected: ManagerSection, choose: (ManagerSection) -> Unit, sections: List<ManagerSection>, modifier: Modifier) {
    Column(modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sections.forEach { item ->
            PosButton(
                label = item.label,
                click = { choose(item) },
                selected = selected == item,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

// Uses a compact selector in portrait.
@Composable
private fun ManagerCompactNav(selected: ManagerSection, choose: (ManagerSection) -> Unit, sections: List<ManagerSection>) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        PosButton("Sección: ${selected.label}", { expanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { sections.forEach { item -> DropdownMenuItem(text = { Text(item.label) }, onClick = { choose(item); expanded = false }) } }
    }
}

// Routes each Manager area while preserving the local session.
@Composable
private fun ManagerSectionContent(section: ManagerSection, shift: ShiftEntity?, manager: LocalUserEntity, users: List<LocalUserEntity>, products: List<ProductEntity>, pendingEvents: Int, summary: DashboardSummary?, repository: PosRepository, refresh: () -> Unit, onShiftClosed: () -> Unit, modifier: Modifier) {
    when (section) {
        ManagerSection.DASHBOARD -> DashboardPanel(summary, pendingEvents, modifier)
        ManagerSection.Z_CLOSE -> if (shift != null) ZClosePanel(shift, manager, users, summary, repository, refresh, onShiftClosed, modifier)
        ManagerSection.HISTORY -> if (shift != null) HistoryPanel(shift, manager, repository, refresh, modifier)
        ManagerSection.PRODUCTS -> ProductsPanel(products, repository, modifier)
        ManagerSection.CONFIG -> ConfigPanel(repository, modifier)
        ManagerSection.STATUS -> StatusPanel(pendingEvents, repository, modifier)
    }
}


// Lays out compact supervision tiles without exposing an accounting ledger.
@Composable
internal fun MetricTiles(metrics: List<Pair<String, String>>) {
    BoxWithConstraints {
        val columns = if (maxWidth >= 540.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            metrics.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, value) -> MetricTile(label, value, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}


// Creates a flat metric tile with a clear numeric hierarchy.
@Composable
internal fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) { Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) } } }

// Keeps empty states explicit instead of rendering a blank surface.
@Composable
internal fun EmptySurface(message: String) { Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) { Text(message, Modifier.fillMaxWidth().padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }

package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
// Local tablet toggles that do not come from server policy sync.
@Composable
internal fun ConfigPanel(repository: PosRepository, modifier: Modifier) {
    var kitchenTicketFilter by remember { mutableStateOf(repository.kitchenTicketPrintFilterEnabled()) }
    var uppercaseNames by remember { mutableStateOf(repository.salesCardNamesUppercase()) }
    var highSaleThreshold by remember { mutableStateOf<Long?>(null) }
    var highChangeThreshold by remember { mutableStateOf<Long?>(null) }
    var policyLoaded by remember { mutableStateOf(false) }
    var devicesOpen by rememberSaveable { mutableStateOf(false) }

    // Reads the cached site policy off the main thread (Room).
    LaunchedEffect(repository) {
        val (sale, change) = withContext(Dispatchers.IO) {
            repository.highSaleWarningCentavos() to repository.highChangeWarningCentavos()
        }
        highSaleThreshold = sale
        highChangeThreshold = change
        policyLoaded = true
    }

    // Reuses the cashier's full-screen device panel; cashiers still open it without PIN from the sale bar.
    if (devicesOpen) {
        Box(modifier) {
            DevicesScreen(
                repository = repository,
                onReturn = { devicesOpen = false },
                returnLabel = "Volver a Configuración",
            )
        }
        return
    }

    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Configuración", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Cada ajuste indica si aplica solo a esta tablet o si es una política de la sede que se cambia en la web.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsBlock("Tickets e impresión") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            kitchenTicketFilter = !kitchenTicketFilter
                            repository.setKitchenTicketPrintFilterEnabled(kitchenTicketFilter)
                        },
                ) {
                    Checkbox(
                        checked = kitchenTicketFilter,
                        onCheckedChange = { checked ->
                            kitchenTicketFilter = checked
                            repository.setKitchenTicketPrintFilterEnabled(checked)
                        },
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            "Imprimir ticket solo para preparación y productos sin código de proveedor",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Chilaquiles y platillos sin barcode de empaque sí imprimen (cocina y cliente). " +
                                "Un carrito solo de empaquetados con barcode (ej. Boing) no imprime ticket, " +
                                "pero en efectivo sí se abre el cajón. Si mezclas empaque y preparación, sí imprime. " +
                                "Desactiva para imprimir todas las ventas.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "La reimpresión desde Historial sigue disponible cuando haga falta un ticket.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsBlock("Dispositivos") {
                Text(
                    "Impresora, lector y pruebas de esta tablet. Los cajeros también lo abren sin PIN desde Acciones → Dispositivos.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PosButton("Administrar dispositivos", { devicesOpen = true }, modifier = Modifier.fillMaxWidth())
            }
            SettingsBlock("Ventas") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            uppercaseNames = !uppercaseNames
                            repository.setSalesCardNamesUppercase(uppercaseNames)
                        },
                ) {
                    Checkbox(
                        checked = uppercaseNames,
                        onCheckedChange = { checked ->
                            uppercaseNames = checked
                            repository.setSalesCardNamesUppercase(checked)
                        },
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("Mostrar nombres en MAYÚSCULAS en el catálogo", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Solo esta tablet",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                SitePolicyThreshold("Advertencia de venta alta", highSaleThreshold, policyLoaded)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                SitePolicyThreshold("Advertencia de cambio alto", highChangeThreshold, policyLoaded)
            }
        }
    }
}

// Read-only site policy threshold; null threshold means the site disabled the warning.
@Composable
private fun SitePolicyThreshold(title: String, thresholdCentavos: Long?, loaded: Boolean) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        if (loaded) {
            Text(
                thresholdCentavos?.let { "Desde ${Money.format(it)}" } ?: "Desactivada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "Política de la sede · Se cambia en Web",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Titled surface that groups related local settings inside Configuración.
@Composable
private fun SettingsBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}


package io.github.alexistrejo.pimienta.pos

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.hardware.PosScannerRegistry
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.domain.*
import io.github.alexistrejo.pimienta.pos.hardware.PosPrinterRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PrinterFactory
import io.github.alexistrejo.pimienta.pos.hardware.printerStatusPresentation
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Polls USB or Bluetooth printer health for the sale status bar.
@Composable
internal fun rememberLivePrinterStatus(context: Context, mode: RuntimeMode): Pair<String, Boolean> {
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(mode) {
        PosPrinterRegistry.statusTick.collect { refresh++ }
    }
    LaunchedEffect(mode) {
        while (true) {
            delay(2_000)
            refresh++
        }
    }
    val presentation = remember(refresh, mode) {
        val availability = PrinterFactory.availability(context, mode)
        printerStatusPresentation(mode, availability.status, availability.link)
    }
    return presentation.label to presentation.alert
}

// Keeps operational status visible in a compact collapsible top bar.
@Composable
internal fun StatusBar(
    cashier: String,
    pending: Int,
    syncLabel: String,
    landscape: Boolean,
    openManager: () -> Unit,
    openDevices: () -> Unit,
    openWithdrawal: () -> Unit,
    withdrawalEnabled: Boolean,
    printerLabel: String,
    printerAlert: Boolean,
    scannerLabel: String,
    deviceVisibleCode: String? = null,
    onCreateProduct: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    syncBusy: Boolean = false,
    createProductEnabled: Boolean = true,
) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val headerBtnPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { expanded = !expanded }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                if (landscape) "Punto de Venta" else "POS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(if (landscape) "Turno: $cashier" else cashier)
                StatusChip(printerLabel, alert = printerAlert)
                if (!expanded && (pending > 0 || syncLabel.contains("desactualizado", ignoreCase = true))) {
                    StatusChip(syncLabel, alert = syncLabel.contains("desactualizado", ignoreCase = true))
                }
            }
            PosButton(
                label = if (expanded) "Ocultar ▲" else "Acciones ▼",
                click = { expanded = !expanded },
                contentPadding = headerBtnPadding,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PosButton("Agregar producto", onCreateProduct, enabled = createProductEnabled, contentPadding = headerBtnPadding)
                    PosButton(if (syncBusy) "Sincronizando…" else "Sincronizar", onSyncNow, enabled = !syncBusy, contentPadding = headerBtnPadding)
                    PosButton("Sangría", openWithdrawal, enabled = withdrawalEnabled, contentPadding = headerBtnPadding)
                    PosButton("Dispositivos", openDevices, contentPadding = headerBtnPadding)
                    PosButton("Panel Manager", openManager, contentPadding = headerBtnPadding)
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusChip(syncLabel, alert = syncLabel.contains("desactualizado", ignoreCase = true))
                    StatusChip(deviceVisibleCode?.takeIf { it.isNotBlank() } ?: "Sin código")
                    StatusChip(scannerLabel)
                }
            }
        }
    }
}


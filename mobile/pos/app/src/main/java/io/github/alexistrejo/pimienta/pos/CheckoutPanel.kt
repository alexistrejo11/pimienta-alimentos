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

// Collects payment methods and cash inputs, positioned cleanly without redundant top headers or vertical scrolling.
@Composable
internal fun Checkout(
    modifier: Modifier,
    total: Long,
    courtesy: Boolean,
    busy: Boolean,
    method: PaymentMethod,
    onMethodChanged: (PaymentMethod) -> Unit,
    tendered: String,
    onTenderedChanged: (String) -> Unit,
    back: () -> Unit,
    confirm: (PaymentMethod, Long) -> Unit,
) {
    val tenderedMoney = Money.fromInput(tendered) ?: 0

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Total a cobrar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(Money.format(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
                if (!courtesy) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PosButton(
                        "Efectivo",
                        { onMethodChanged(PaymentMethod.CASH) },
                        method == PaymentMethod.CASH,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    )
                    PosButton(
                        "Tarjeta externa",
                        { onMethodChanged(PaymentMethod.EXTERNAL_CARD_MP) },
                        method == PaymentMethod.EXTERNAL_CARD_MP,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (courtesy) {
                    Text("Cortesía autorizada · no se recibe efectivo ni tarjeta", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (method == PaymentMethod.CASH) {
                    CashPayment(tendered, onTenderedChanged, total)
                } else {
                    ExternalCardNotice()
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PosButton(
                    "Volver",
                    back,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                )
                PosButton(
                    if (busy) "Confirmando…" else if (courtesy) "Confirmar cortesía" else "Confirmar cobro",
                    { confirm(if (courtesy) PaymentMethod.CORTESIA else method, if (courtesy) 0 else if (method == PaymentMethod.CASH) tenderedMoney else total) },
                    enabled = !busy && (courtesy || method != PaymentMethod.CASH || tenderedMoney >= total),
                    primary = true,
                    modifier = Modifier.weight(1.5f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}


// Combines numeric entry, denomination shortcuts, and the resulting change for cash.
@Composable
internal fun CashPayment(value: String, changed: (String) -> Unit, total: Long) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val twoColumns = maxWidth >= 480.dp
        if (twoColumns) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CashEntry(Modifier.weight(1f), value, changed, total)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CashEntry(Modifier.fillMaxWidth(), value, changed, total)
            }
        }
    }
}


// Renders direct numeric entry for cash without calling the system keyboard.
@Composable
internal fun CashEntry(modifier: Modifier, value: String, changed: (String) -> Unit, total: Long = 0) {
    val received = Money.fromInput(value) ?: 0
    val change = SaleCalculator.changeCentavos(received, total).coerceAtLeast(0)

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Efectivo recibido", style = MaterialTheme.typography.labelMedium)
            val displayMoney = if (value.isBlank()) Money.format(0) else Money.format(received)
            Text(displayMoney, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (received > total) {
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cambio a entregar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(Money.format(change), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Spacer(Modifier.height(4.dp))
        Numpad(value, changed, decimal = true, showDisplay = false, keyHeight = 36.dp, keySpacing = 4.dp)
    }
}


// Explains the manual approval boundary for the external physical terminal.
@Composable
internal fun ExternalCardNotice() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Terminal externa", style = MaterialTheme.typography.titleMedium)
            Text(
                "Confirma este cobro solo después de que Mercado Pago apruebe la operación en su terminal física.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Esta versión no se conecta a la terminal.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}


// Brief confirmation that auto-hides; a new folio replaces the message and restarts the timer.
@Composable
internal fun SaleCompleted(folio: String?, onFinished: (String) -> Unit) {
    var displayed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(folio) {
        if (folio != null) {
            displayed = folio
            delay(3_500)
            onFinished(folio)
        }
    }
    AnimatedVisibility(
        visible = folio != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Venta confirmada · ${displayed.orEmpty()} · Ticket en cola de impresión",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}


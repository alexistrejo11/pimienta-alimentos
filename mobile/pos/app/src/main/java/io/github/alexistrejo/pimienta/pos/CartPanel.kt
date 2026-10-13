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

// Displays the editable draft sale and leaves payment to the next workspace state.
@Composable
internal fun CartPanel(
    modifier: Modifier,
    cart: List<CartLine>,
    discount: SaleDiscountDraft?,
    onChange: (List<CartLine>) -> Unit,
    applyDiscount: () -> Unit,
    onShowCatalog: (() -> Unit)? = null,
    checkout: () -> Unit,
) {
    val gross = cart.totalCentavos()
    val discountAmount = discount?.amountCentavos ?: 0
    val total = SaleCalculator.netCentavos(gross, discountAmount)
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Venta activa", style = MaterialTheme.typography.titleMedium)
                    Text("Folio al confirmar · ${cart.sumOf { it.quantity }} artículos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onShowCatalog != null) {
                    PosButton(
                        label = "◀ Ver catálogo",
                        click = onShowCatalog,
                        primary = true,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))

            if (cart.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Agrega productos del catálogo para iniciar una venta.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(cart, key = { it.lineKey }) { line ->
                        val quantityLocked = line.lineType == SaleLineType.OPEN_AMOUNT
                        CartLineRow(
                            line,
                            decrease = {
                                onChange(cart.mapNotNull { current ->
                                    if (current.lineKey != line.lineKey) current
                                    else if (quantityLocked || current.quantity == 1) null
                                    else current.copy(quantity = current.quantity - 1)
                                })
                            },
                            increase = {
                                if (!quantityLocked) {
                                    onChange(cart.map { current ->
                                        if (current.lineKey == line.lineKey) current.copy(quantity = current.quantity + 1) else current
                                    })
                                }
                            },
                            quantityAdjustable = !quantityLocked,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (discount == null) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text(Money.format(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        TotalLine("Venta bruta", Money.format(gross))
                        TotalLine("Descuento · ${discount.reason}", "− ${Money.format(discountAmount)}")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        TotalLine("Total neto", Money.format(total), emphasized = true)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosButton("Limpiar", { onChange(emptyList()) }, enabled = cart.isNotEmpty(), modifier = Modifier.weight(0.8f))
                PosButton("Descuento / cortesía", applyDiscount, enabled = cart.isNotEmpty(), modifier = Modifier.weight(1.2f))
            }
            Spacer(Modifier.height(6.dp))
            PosButton(
                "Cobrar ${Money.format(total)}",
                checkout,
                enabled = cart.isNotEmpty(),
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}


// Aligns monetary summary labels and values in compact operational rows.
@Composable
private fun TotalLine(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}


// Keeps quantities inline so a cashier can adjust a line without navigating away.
@Composable
internal fun CartLineRow(
    line: CartLine,
    decrease: () -> Unit,
    increase: () -> Unit,
    quantityAdjustable: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${line.quantity} × ${Money.format(line.unitPriceCentavos)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Money.format(line.subtotalCentavos), fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        QuantityButton("−", decrease)
        Text("${line.quantity}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
        QuantityButton("+", increase, enabled = quantityAdjustable)
    }
}


// Provides a compact touch target for cart quantities without a pill-shaped control.
@Composable
internal fun QuantityButton(label: String, click: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = click,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}


// Sums the immutable price snapshots already captured in the draft cart.
internal fun List<CartLine>.totalCentavos(): Long = SaleCalculator.grossCentavos(this)

// Supplies filled inputs with the POS surface treatment instead of a heavy outline.

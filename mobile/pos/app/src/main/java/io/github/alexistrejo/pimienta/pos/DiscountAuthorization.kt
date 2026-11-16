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

// Authorizes one of the three discount modes while preserving the active cashier cart.
@Composable
internal fun DiscountAuthorization(
    users: List<LocalUserEntity>,
    lines: List<CartLine>,
    current: SaleDiscountDraft?,
    repository: PosRepository,
    onDismiss: () -> Unit,
    onAuthorized: (SaleDiscountDraft) -> Unit,
) {
    val managers = users.filter { it.isManagerOrAdmin }
    val gross = SaleCalculator.grossCentavos(lines)
    var mode by remember { mutableStateOf(current?.mode ?: DiscountMode.TARGET_AMOUNT) }
    var selected by remember { mutableStateOf(current?.authorizedBy ?: managers.firstOrNull()) }
    var targetText by remember {
        mutableStateOf(current?.targetCentavos?.let { BigDecimal.valueOf(it, 2).toPlainString() } ?: "")
    }
    var reason by remember { mutableStateOf(current?.reason ?: "") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val (targetInteraction, forceTargetKeyboard) = rememberForceSoftKeyboardInteractionSource()
    val (reasonInteraction, forceReasonKeyboard) = rememberForceSoftKeyboardInteractionSource()
    val targetCentavos = Money.fromInput(targetText)
    val quote = SaleCalculator.quote(mode, lines, if (mode == DiscountMode.TARGET_AMOUNT) targetCentavos else null)
    val accepted = quote as? DiscountQuote.Accepted
    val warning = discountWarning(mode, targetText, targetCentavos, quote)

    // Uses the same validation for the keypad enter key and the primary action.
    fun authorize() {
        val user = selected
        val priced = accepted
        when {
            priced == null -> error = warning ?: "Este descuento no se puede aplicar."
            reason.isBlank() -> error = "El motivo es obligatorio."
            user == null -> error = "Selecciona un autorizador."
            else -> scope.launch {
                val valid = withContext(Dispatchers.IO) { repository.authenticate(user.id, pin) }
                if (valid) {
                    onAuthorized(
                        SaleDiscountDraft(
                            amountCentavos = priced.discountCentavos,
                            reason = reason.trim(),
                            authorizedBy = user,
                            mode = priced.mode,
                            targetCentavos = if (priced.mode == DiscountMode.TARGET_AMOUNT) priced.chargeCentavos else null,
                        )
                    )
                } else {
                    error = "El PIN no corresponde al perfil seleccionado."
                }
            }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Descuento / cortesía", style = MaterialTheme.typography.titleLarge)
                Text("Venta bruta: ${Money.format(gross)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                DiscountMode.entries.forEach { option ->
                    PosButton(
                        option.dialogLabel(),
                        { mode = option; error = null },
                        selected = mode == option,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (mode == DiscountMode.TARGET_AMOUNT) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it; error = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (it.isFocused) forceTargetKeyboard() },
                        interactionSource = targetInteraction,
                        label = { Text("Monto a cobrar") },
                        singleLine = true,
                        colors = formFieldColors(),
                    )
                }
                DiscountQuoteSummary(mode, gross, accepted)
                warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) forceReasonKeyboard() },
                    interactionSource = reasonInteraction,
                    label = { Text("Motivo obligatorio") },
                    singleLine = true,
                    colors = catalogFieldColors(),
                )
                val isTraining = repository.mode() == RuntimeMode.SANDBOX
                managers.forEach { user -> PosButton(user.displayTitle(isTraining), { selected = user }, selected = selected?.id == user.id, modifier = Modifier.fillMaxWidth()) }
                Text("PIN de Gerente o Administrador", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton(
                        "Autorizar",
                        ::authorize,
                        enabled = accepted != null && reason.isNotBlank() && selected != null,
                        primary = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// Live charge and discount for the mode the operator is about to authorize.
@Composable
private fun DiscountQuoteSummary(mode: DiscountMode, gross: Long, accepted: DiscountQuote.Accepted?) {
    if (accepted == null && mode != DiscountMode.TARGET_AMOUNT) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (mode == DiscountMode.TARGET_AMOUNT) {
            Text("Subtotal: ${Money.format(gross)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (accepted != null) {
            val discountLabel = if (mode == DiscountMode.FULL_COURTESY) {
                "${Money.format(accepted.discountCentavos)} (100%)"
            } else {
                Money.format(accepted.discountCentavos)
            }
            if (mode != DiscountMode.TARGET_AMOUNT) {
                Text("Monto a cobrar: ${Money.format(accepted.chargeCentavos)}")
            }
            Text("Descuento: $discountLabel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (mode == DiscountMode.TARGET_AMOUNT) {
                Text("Monto a cobrar: ${Money.format(accepted.chargeCentavos)}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Selector copy. The cart uses a shorter label for a custom charge.
private fun DiscountMode.dialogLabel(): String = when (this) {
    DiscountMode.FULL_COURTESY -> "Cortesía total"
    DiscountMode.AT_COST -> "Cobro al costo"
    DiscountMode.TARGET_AMOUNT -> "Monto a cobrar"
}

// Explains why Autorizar stays disabled, including a $0 target that belongs on cortesía total.
private fun discountWarning(
    mode: DiscountMode,
    targetText: String,
    targetCentavos: Long?,
    quote: DiscountQuote,
): String? {
    if (mode == DiscountMode.TARGET_AMOUNT) {
        when {
            targetText.isBlank() -> return "Captura el monto a cobrar."
            targetCentavos == null -> return "Captura un monto válido."
            targetCentavos == 0L -> return "Para cobrar $0 selecciona Cortesía total."
        }
    }
    return (quote as? DiscountQuote.Blocked)?.let { discountBlockMessage(it.block, it.lineNames) }
}


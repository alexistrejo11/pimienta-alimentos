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

// Authorizes one fixed-amount discount while preserving the active cashier cart.
@Composable
internal fun DiscountAuthorization(
    users: List<LocalUserEntity>,
    gross: Long,
    current: SaleDiscountDraft?,
    repository: PosRepository,
    onDismiss: () -> Unit,
    onAuthorized: (SaleDiscountDraft) -> Unit,
) {
    val managers = users.filter { it.isManagerOrAdmin }
    var selected by remember { mutableStateOf(managers.firstOrNull()) }
    var amount by remember { mutableStateOf(current?.amountCentavos?.let { BigDecimal.valueOf(it, 2).toPlainString() } ?: "") }
    var reason by remember { mutableStateOf(current?.reason ?: "") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val (amountInteraction, forceAmountKeyboard) = rememberForceSoftKeyboardInteractionSource()
    val (reasonInteraction, forceReasonKeyboard) = rememberForceSoftKeyboardInteractionSource()

    // Uses the same validation for the keypad enter key and the primary action.
    fun authorize() {
        val cents = Money.fromInput(amount)
        val user = selected
        when {
            cents == null || cents <= 0 -> error = "Captura un importe válido."
            cents > gross -> error = "El descuento no puede superar la venta."
            reason.isBlank() -> error = "El motivo es obligatorio."
            user == null -> error = "Selecciona un autorizador."
            else -> scope.launch {
                val valid = withContext(Dispatchers.IO) { repository.authenticate(user.id, pin) }
                if (valid) onAuthorized(SaleDiscountDraft(cents, reason.trim(), user)) else error = "El PIN no corresponde al perfil seleccionado."
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
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) forceAmountKeyboard() },
                    interactionSource = amountInteraction,
                    label = { Text("Importe fijo en pesos") },
                    singleLine = true,
                    colors = catalogFieldColors(),
                )
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
                    PosButton("Autorizar", ::authorize, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


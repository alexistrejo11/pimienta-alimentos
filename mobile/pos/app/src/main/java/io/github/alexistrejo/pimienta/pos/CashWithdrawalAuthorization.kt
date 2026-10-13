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

// Captures a fixed safeguard withdrawal and requires an in-person manager signature.
@Composable
internal fun CashWithdrawalAuthorization(
    users: List<LocalUserEntity>,
    repository: PosRepository,
    shift: ShiftEntity,
    onDismiss: () -> Unit,
    onRecorded: (CashWithdrawalEntity) -> Unit,
) {
    val managers = users.filter { it.isManagerOrAdmin }
    var selected by remember { mutableStateOf(managers.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    // Blocks a second tap or keypad enter while the first withdrawal is still being saved.
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Records the same withdrawal from either keypad enter or the visible action.
    fun record() {
        if (submitting) return
        val cents = Money.fromInput(amount)
        val user = selected
        if (cents == null || cents <= 0) error = "Captura un importe válido."
        else if (user == null) error = "Selecciona un autorizador."
        else {
            submitting = true
            scope.launch {
                val withdrawal = withContext(Dispatchers.IO) { repository.recordWithdrawal(shift, cents, user.id, pin) }
                if (withdrawal == null) {
                    error = "No se pudo autorizar la sangría."
                    submitting = false
                } else {
                    PrintWorker.enqueue(context)
                    SyncWorker.enqueue(context)
                    onRecorded(withdrawal)
                }
            }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            // Allows the dialog content to scroll when multiple manager profiles are present
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Registrar sangría", style = MaterialTheme.typography.titleLarge)
                Text("Motivo: Resguardo de efectivo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Importe en pesos", style = MaterialTheme.typography.labelLarge)
                Numpad(amount, { amount = it }, decimal = true, onSubmit = ::record)
                val isTraining = repository.mode() == RuntimeMode.SANDBOX
                managers.forEach { user -> PosButton(user.displayTitle(isTraining), { selected = user }, selected = selected?.id == user.id, modifier = Modifier.fillMaxWidth()) }
                Text("PIN de Gerente o Administrador", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Confirmar e imprimir", ::record, enabled = !submitting, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}




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

// Creates a POS-sellable product (server in production, local scratch in training).
@Composable
internal fun CreatePosProductDialog(
    categories: List<String>,
    lockedBarcode: String? = null,
    initialPriceCentavos: Long? = null,
    sandbox: Boolean,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, category: String, priceCentavos: Long, barcode: String?, controlled: Boolean) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(initialPriceCentavos?.let { BigDecimal.valueOf(it, 2).toPlainString() } ?: "") }
    var barcode by rememberSaveable { mutableStateOf(lockedBarcode.orEmpty()) }
    var controlled by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    val (nameInteraction, forceNameKeyboard) = rememberForceSoftKeyboardInteractionSource()
    val (barcodeInteraction, forceBarcodeKeyboard) = rememberForceSoftKeyboardInteractionSource()

    val scanner = remember { PosScannerRegistry.primary }
    LaunchedEffect(scanner, lockedBarcode) {
        if (scanner == null || lockedBarcode != null) return@LaunchedEffect
        scanner.start()
        scanner.events.collect { read ->
            val code = read.rawValue.trim()
            if (code.isNotBlank()) {
                barcode = code
            }
        }
    }

    fun submit() {
        val cents = Money.fromInput(amount)
        when {
            name.isBlank() -> localError = "Captura el nombre."
            category.isBlank() -> localError = "Elige una categoría de venta."
            cents == null || cents <= 0 -> localError = "Captura un precio válido."
            else -> {
                localError = null
                onSubmit(name.trim(), category, cents, barcode.trim().ifBlank { null }, controlled)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Nuevo producto", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (sandbox) {
                        "Capacitación: se guarda en esta tablet y desaparece al salir o al reiniciar. No se envía al servidor."
                    } else {
                        "Sin código de barras queda como producto interno."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) forceNameKeyboard() },
                    interactionSource = nameInteraction,
                    label = { Text("Nombre") },
                    singleLine = true,
                    colors = formFieldColors(),
                )
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        PosButton(cat, { category = cat }, selected = category == cat)
                    }
                }
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { if (lockedBarcode == null) barcode = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) forceBarcodeKeyboard() },
                    interactionSource = barcodeInteraction,
                    label = { Text("Código de barras (opcional)") },
                    singleLine = true,
                    readOnly = lockedBarcode != null,
                    colors = formFieldColors(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = controlled, onCheckedChange = { controlled = it })
                    Text("Controla inventario")
                }
                Text("Precio", style = MaterialTheme.typography.labelLarge)
                Numpad(amount, { amount = it }, decimal = true, onSubmit = ::submit)
                (error ?: localError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f), enabled = !busy)
                    PosButton(if (busy) "Guardando…" else "Guardar", ::submit, primary = true, modifier = Modifier.weight(1f), enabled = !busy)
                }
            }
        }
    }
}


// Edits name, category, price, and stock control for one site product.
@Composable
internal fun EditPosProductDialog(
    productName: String,
    categories: List<String>,
    category: String,
    initialBarcode: String,
    initialPriceCentavos: Long,
    initialControlled: Boolean,
    sandbox: Boolean,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, category: String, priceCentavos: Long, controlled: Boolean, barcode: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(productName) }
    var selectedCategory by rememberSaveable { mutableStateOf(category) }
    var barcode by rememberSaveable { mutableStateOf(initialBarcode) }
    var amount by rememberSaveable { mutableStateOf(BigDecimal.valueOf(initialPriceCentavos, 2).toPlainString()) }
    var controlled by rememberSaveable { mutableStateOf(initialControlled) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    val (nameInteraction, forceNameKeyboard) = rememberForceSoftKeyboardInteractionSource()
    val (barcodeInteraction, forceBarcodeKeyboard) = rememberForceSoftKeyboardInteractionSource()

    val scanner = remember { PosScannerRegistry.primary }
    LaunchedEffect(scanner, busy) {
        if (scanner == null || busy) return@LaunchedEffect
        scanner.start()
        scanner.events.collect { read ->
            val code = read.rawValue.trim()
            if (code.isNotBlank()) {
                barcode = code
            }
        }
    }

    fun submit() {
        if (busy) return
        val cents = Money.fromInput(amount)
        when {
            name.isBlank() -> localError = "Captura el nombre."
            selectedCategory.isBlank() -> localError = "Elige una categoría de venta."
            cents == null || cents <= 0 -> localError = "Captura un precio válido."
            else -> {
                localError = null
                onSubmit(name.trim(), selectedCategory, cents, controlled, barcode.trim())
            }
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Editar producto", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (sandbox) {
                        "Capacitación: el cambio queda en esta tablet y desaparece al salir o al reiniciar."
                    } else {
                        "Nombre y código de barras se comparten. Categoría, precio e inventario son de esta sede."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) forceNameKeyboard() },
                    interactionSource = nameInteraction,
                    label = { Text("Nombre") },
                    singleLine = true,
                    enabled = !busy,
                    colors = formFieldColors(),
                )
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) forceBarcodeKeyboard() },
                    interactionSource = barcodeInteraction,
                    label = { Text("Código de barras") },
                    supportingText = { Text("Opcional. Vacío significa que no tiene código de empaque.") },
                    singleLine = true,
                    enabled = !busy,
                    colors = formFieldColors(),
                )
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        PosButton(cat, { if (!busy) selectedCategory = cat }, selected = selectedCategory == cat, enabled = !busy)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = controlled, onCheckedChange = { controlled = it }, enabled = !busy)
                    Text("Controla inventario")
                }
                Text("Precio", style = MaterialTheme.typography.labelLarge)
                Numpad(amount, { if (!busy) amount = it }, decimal = true, onSubmit = ::submit)
                (error ?: localError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f), enabled = !busy)
                    PosButton(if (busy) "Guardando…" else "Guardar", ::submit, primary = true, modifier = Modifier.weight(1f), enabled = !busy)
                }
            }
        }
    }
}


package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import io.github.alexistrejo.pimienta.pos.domain.*
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Keeps operational status visible without letting a portrait header overflow.
@Composable
internal fun StatusBar(
    cashier: String,
    pending: Int,
    dark: Boolean,
    onTheme: (Boolean) -> Unit,
    landscape: Boolean,
    lockCashRegister: () -> Unit,
    openManager: () -> Unit,
    openWithdrawal: () -> Unit,
    withdrawalEnabled: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(48.dp).padding(end = 8.dp)
            )
            Text("Punto de Venta", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            PosButton("Sangría", openWithdrawal, enabled = withdrawalEnabled)
            Spacer(Modifier.width(8.dp))
            PosButton("Bloquear caja", lockCashRegister)
            Spacer(Modifier.width(8.dp))
            PosButton("Panel Manager", openManager)
            Spacer(Modifier.width(8.dp))
            if (BuildConfig.DEBUG) {
                PosButton(if (dark) "Tema claro" else "Tema oscuro", { onTheme(!dark) })
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusChip("Sin conexión · $pending pendientes")
            StatusChip("Tablet T1")
            StatusChip(if (landscape) "Turno abierto · $cashier" else "Turno abierto")
            StatusChip("Impresora lista")
        }
    }
}

// Captures a fixed safeguard withdrawal and requires an in-person manager signature.
@Composable
internal fun CashWithdrawalAuthorization(
    users: List<LocalUserEntity>,
    repository: PosRepository,
    shift: ShiftEntity,
    onDismiss: () -> Unit,
    onRecorded: (CashWithdrawalEntity) -> Unit,
) {
    val managers = users.filter { it.role == "MANAGER" || it.role == "SUPERADMIN" }
    var selected by remember { mutableStateOf(managers.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Records the same withdrawal from either keypad enter or the visible action.
    fun record() {
        val cents = Money.fromInput(amount)
        val user = selected
        if (cents == null || cents <= 0) error = "Captura un importe válido."
        else if (user == null) error = "Selecciona un autorizador."
        else scope.launch {
            val withdrawal = withContext(Dispatchers.IO) { repository.recordWithdrawal(shift, cents, user.id, pin) }
            if (withdrawal == null) error = "No se pudo autorizar la sangría." else onRecorded(withdrawal)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.widthIn(max = 520.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Registrar sangría", style = MaterialTheme.typography.titleLarge)
                Text("Motivo: Resguardo de efectivo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(amount, { amount = it }, label = { Text("Importe en pesos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                managers.forEach { user -> PosButton(user.displayName, { selected = user }, selected = selected?.id == user.id, modifier = Modifier.fillMaxWidth()) }
                Text("PIN de Manager/Superadmin", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true, onSubmit = ::record)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Confirmar e imprimir", ::record, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Hides sale data until the cashier responsible for the active shift verifies their PIN.
@Composable
internal fun LockedCashRegister(
    repository: PosRepository,
    cashierId: String,
    cashierName: String,
    onUnlocked: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Reuses the visible action and keypad enter key for the same PIN validation.
    fun unlock() {
        if (busy || pin.isBlank()) return
        scope.launch {
            busy = true
            val valid = withContext(Dispatchers.IO) { repository.authenticate(cashierId, pin) }
            busy = false
            if (valid) onUnlocked() else error = "El PIN no corresponde al cajero responsable."
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                )
                Text("Pimienta POS", style = MaterialTheme.typography.headlineSmall)
                Text("Caja bloqueada", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Turno activo · Cajero responsable: $cashierName",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("PIN", style = MaterialTheme.typography.titleMedium)
                Numpad(pin, { pin = it }, masked = true, onSubmit = ::unlock)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                PosButton(
                    label = if (busy) "Desbloqueando…" else "Desbloquear caja",
                    click = ::unlock,
                    enabled = !busy && pin.isNotBlank(),
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "El turno y el carrito continúan resguardados en la tablet.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Displays one small operational status without presenting it as a dashboard card.
@Composable
internal fun StatusChip(label: String) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

// Lets portrait users switch between catalog and cart while preserving one draft sale.
@Composable
internal fun PortraitTabs(selected: PortraitPanel, itemCount: Int, onSelect: (PortraitPanel) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosButton("Catálogo", { onSelect(PortraitPanel.CATALOG) }, selected == PortraitPanel.CATALOG, modifier = Modifier.weight(1f))
        PosButton("Carrito · $itemCount", { onSelect(PortraitPanel.CART) }, selected == PortraitPanel.CART, modifier = Modifier.weight(1f))
    }
}

// Shows searchable catalog products as flat touch targets rather than elevated Material cards.
@Composable
internal fun CatalogPanel(
    modifier: Modifier,
    categories: List<String>,
    selectedCategory: String,
    onCategory: (String) -> Unit,
    search: String,
    onSearch: (String) -> Unit,
    products: List<ProductEntity>,
    onProduct: (ProductEntity) -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text("Catálogo", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por nombre o código") },
                singleLine = true,
                colors = catalogFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it }) { item ->
                    PosButton(item, { onCategory(item) }, selected = selectedCategory == item)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(10.dp))

            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay productos para esta búsqueda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(products, key = { it.id }) { ProductTile(it) { onProduct(it) } }
                }
            }
        }
    }
}

// Shows product price and stock context in one compact, high-target catalog tile.
@Composable
internal fun ProductTile(product: ProductEntity, onClick: () -> Unit) {
    val unavailable = !product.available
    val stock = product.stock.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val controlled = product.stockPolicy == "CONTROLLED"
    val warning = controlled && stock <= BigDecimal.ZERO
    val status = when {
        unavailable -> "No disponible"
        !controlled -> "Preparado al momento"
        warning -> "Inventario local: ${product.stock}"
        else -> "${product.stock} disponibles"
    }
    val statusColor = if (unavailable || warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 116.dp),
        enabled = !unavailable,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Column {
                Text(Money.format(Money.fromCatalog(product.price)), fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.labelMedium, color = statusColor)
            }
        }
    }
}

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
    val managers = users.filter { it.role == "MANAGER" || it.role == "SUPERADMIN" }
    var selected by remember { mutableStateOf(managers.firstOrNull()) }
    var amount by remember { mutableStateOf(current?.amountCentavos?.let { BigDecimal.valueOf(it, 2).toPlainString() } ?: "") }
    var reason by remember { mutableStateOf(current?.reason ?: "") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
            Column(Modifier.widthIn(max = 520.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Descuento / cortesía", style = MaterialTheme.typography.titleLarge)
                Text("Venta bruta: ${Money.format(gross)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(amount, { amount = it }, label = { Text("Importe fijo en pesos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reason, { reason = it }, label = { Text("Motivo obligatorio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                managers.forEach { user -> PosButton(user.displayName, { selected = user }, selected = selected?.id == user.id, modifier = Modifier.fillMaxWidth()) }
                Text("PIN de Manager/Superadmin", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true, onSubmit = ::authorize)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton("Autorizar", ::authorize, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Displays the editable draft sale and leaves payment to the next workspace state.
@Composable
internal fun CartPanel(
    modifier: Modifier,
    cart: List<CartLine>,
    discount: SaleDiscountDraft?,
    onChange: (List<CartLine>) -> Unit,
    applyDiscount: () -> Unit,
    checkout: () -> Unit,
) {
    val gross = cart.totalCentavos()
    val discountAmount = discount?.amountCentavos ?: 0
    val total = gross - discountAmount
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text("Venta activa", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Folio al confirmar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${cart.sumOf { it.quantity }} artículos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

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
                    items(cart, key = { it.productId }) { line ->
                        CartLineRow(
                            line,
                            decrease = {
                                onChange(cart.mapNotNull { current ->
                                    if (current.productId != line.productId) current
                                    else if (current.quantity == 1) null
                                    else current.copy(quantity = current.quantity - 1)
                                })
                            },
                            increase = {
                                onChange(cart.map { current ->
                                    if (current.productId == line.productId) current.copy(quantity = current.quantity + 1) else current
                                })
                            },
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
internal fun CartLineRow(line: CartLine, decrease: () -> Unit, increase: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${line.quantity} × ${Money.format(line.unitPriceCentavos)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Money.format(line.unitPriceCentavos * line.quantity), fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        QuantityButton("−", decrease)
        Text("${line.quantity}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
        QuantityButton("+", increase)
    }
}

// Provides a compact touch target for cart quantities without a pill-shaped control.
@Composable
internal fun QuantityButton(label: String, click: () -> Unit) {
    Button(
        onClick = click,
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

// Collects payment in a scrollable center area with fixed cancel and confirm actions.
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
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            PosButton("Volver al carrito", back, enabled = !busy, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Cobro", style = MaterialTheme.typography.titleLarge)
                Text("Total a cobrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.format(total), style = MaterialTheme.typography.headlineSmall)
                if (!courtesy) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Efectivo", { onMethodChanged(PaymentMethod.CASH) }, method == PaymentMethod.CASH, modifier = Modifier.weight(1f))
                    PosButton("Tarjeta externa", { onMethodChanged(PaymentMethod.EXTERNAL_CARD_MP) }, method == PaymentMethod.EXTERNAL_CARD_MP, modifier = Modifier.weight(1f))
                }
                if (courtesy) {
                    Text("Cortesía autorizada · no se recibe efectivo ni tarjeta", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (method == PaymentMethod.CASH) CashPayment(tendered, onTenderedChanged, total) else ExternalCardNotice()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosButton("Cancelar intento", back, enabled = !busy, modifier = Modifier.weight(1f))
                PosButton(
                    if (busy) "Confirmando…" else if (courtesy) "Confirmar cortesía e imprimir" else "Confirmar cobro e imprimir",
                    { confirm(if (courtesy) PaymentMethod.CORTESIA else method, if (courtesy) 0 else if (method == PaymentMethod.CASH) tenderedMoney else total) },
                    enabled = !busy && (courtesy || method != PaymentMethod.CASH || tenderedMoney >= total),
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// Combines numeric entry, denomination shortcuts, and the resulting change for cash.
@Composable
internal fun CashPayment(value: String, changed: (String) -> Unit, total: Long) {
    val received = Money.fromInput(value) ?: 0
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val twoColumns = maxWidth >= 480.dp
        if (twoColumns) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CashEntry(Modifier.weight(1f), value, changed)
                CashShortcuts(Modifier.weight(1f), received, total, changed)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CashEntry(Modifier.fillMaxWidth(), value, changed)
                CashShortcuts(Modifier.fillMaxWidth(), received, total, changed)
            }
        }
    }
}

// Renders direct numeric entry for cash without calling the system keyboard.
@Composable
internal fun CashEntry(modifier: Modifier, value: String, changed: (String) -> Unit) {
    Column(modifier) {
        Text("Efectivo recibido", style = MaterialTheme.typography.labelLarge)
        Text(Money.format(Money.fromInput(value) ?: 0), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Numpad(value, changed)
    }
}

// Adds common Mexican denominations and exposes the calculated change.
@Composable
internal fun CashShortcuts(modifier: Modifier, received: Long, total: Long, changed: (String) -> Unit) {
    Column(modifier) {
        Text("Billetes y monedas", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        DenominationGrid(
            add = { denomination -> changed(moneyInput(received + denomination)) },
            exact = { changed(moneyInput(total)) },
        )
        Spacer(Modifier.height(12.dp))
        ChangeSummary((received - total).coerceAtLeast(0))
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

// Presents denomination shortcuts that add to the received cash amount.
@Composable
internal fun DenominationGrid(add: (Long) -> Unit, exact: () -> Unit) {
    val denominations = listOf(50L, 100L, 500L, 1_000L, 2_000L, 5_000L, 10_000L, 20_000L, 50_000L)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(172.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(denominations) { denomination ->
            PosButton(Money.format(denomination), { add(denomination) }, modifier = Modifier.fillMaxWidth())
        }
        item { PosButton("Exacto", exact, modifier = Modifier.fillMaxWidth()) }
    }
}

// Highlights change in a flat operational container rather than a modal confirmation.
@Composable
internal fun ChangeSummary(change: Long) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Cambio a entregar", style = MaterialTheme.typography.labelLarge)
            Text(Money.format(change), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// Announces a committed local sale without hiding the workspace behind a dialog.
@Composable
internal fun SaleCompleted(folio: String, dismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Venta confirmada · $folio · Ticket en cola de impresión",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
        Button(
            onClick = dismiss,
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("Nueva venta")
        }
    }
}

// Provides number-only input without invoking the Android keyboard during cashier workflows.
@Composable
internal fun Numpad(value: String, changed: (String) -> Unit, masked: Boolean = false, onSubmit: (() -> Unit)? = null) {
    val keys = if (masked) listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", "⌫", "Entrar") else listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "⌫")
    if (masked) {
        Text(if (value.isBlank()) "PIN vacío" else "•".repeat(value.length), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(218.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items(keys) { key ->
            PosButton(
                key,
                {
                    when (key) {
                        "⌫" -> changed(value.dropLast(1))
                        "Entrar" -> onSubmit?.invoke()
                        "." -> if (!value.contains('.')) changed(if (value.isBlank()) "0." else "$value.")
                        else -> if (value.length < 8) changed(value + key)
                    }
                },
                enabled = key != "Entrar" || (onSubmit != null && value.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            )
        }
    }
}

// Formats centavos as a numeric value that can be read back by Money.fromInput.
internal fun moneyInput(centavos: Long): String = BigDecimal.valueOf(centavos, 2).stripTrailingZeros().toPlainString()

// Sums the immutable price snapshots already captured in the draft cart.
internal fun List<CartLine>.totalCentavos(): Long = sumOf { it.unitPriceCentavos * it.quantity }

// Supplies filled inputs with the POS surface treatment instead of a heavy outline.
@Composable
internal fun catalogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
)

// Standardizes compact, square-cornered actions across the operational workspace.
@Composable
internal fun PosButton(
    label: String,
    click: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val emphasized = primary || selected
    Button(
        onClick = click,
        modifier = modifier.heightIn(min = 42.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
        border = if (!emphasized) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

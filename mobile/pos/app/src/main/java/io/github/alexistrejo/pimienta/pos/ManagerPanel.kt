package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Identifies the four local Manager areas defined in the POS wireframe.
private enum class ManagerSection(val label: String) {
    Z_CLOSE("Corte Z"), INVENTORY("Inventario"), HISTORY("Historial"), STATUS("Estado")
}

// Tracks the visual-only stages of the blind-count prototype.
private enum class CountStage { OPEN, COUNTING, VALIDATION }

// Requests an in-person Manager authorization without changing the cashier session.
@Composable
internal fun ManagerAccess(
    users: List<LocalUserEntity>,
    repository: PosRepository,
    onDismiss: () -> Unit,
    onAuthorized: (LocalUserEntity) -> Unit,
) {
    val managers = users.filter { it.role == "MANAGER" || it.role == "SUPERADMIN" }
    var selected by remember { mutableStateOf(managers.firstOrNull()) }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Autorizar acceso a Manager", style = MaterialTheme.typography.titleLarge)
                Text("La venta y el carrito de la cajera permanecerán activos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                managers.forEach { user ->
                    PosButton(user.displayName, { selected = user }, selected = selected?.id == user.id, modifier = Modifier.fillMaxWidth())
                }
                Text("PIN de Manager", style = MaterialTheme.typography.labelLarge)
                Numpad(pin, { pin = it }, masked = true)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton("Cancelar", onDismiss, modifier = Modifier.weight(1f))
                    PosButton(
                        "Autorizar",
                        {
                            selected?.let { user ->
                                scope.launch {
                                    val valid = withContext(Dispatchers.IO) { repository.authenticate(user.id, pin) }
                                    if (valid) onAuthorized(user) else message = "El PIN no corresponde al perfil seleccionado."
                                }
                            }
                        },
                        primary = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// Renders the early visual prototype of the offline local Manager workspace.
@Composable
internal fun ManagerPanel(
    shift: ShiftEntity,
    manager: LocalUserEntity,
    products: List<ProductEntity>,
    pendingEvents: Int,
    onReturnToSale: () -> Unit,
) {
    var section by remember { mutableStateOf(ManagerSection.Z_CLOSE) }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val landscape = maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            ManagerHeader(shift, manager, onReturnToSale)
            if (landscape) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    ManagerSideNav(section, { section = it }, Modifier.width(172.dp).fillMaxHeight())
                    HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                    ManagerSectionContent(section, shift, products, pendingEvents, Modifier.weight(1f))
                }
            } else {
                ManagerCompactNav(section, { section = it })
                ManagerSectionContent(section, shift, products, pendingEvents, Modifier.weight(1f))
            }
        }
    }
}

// Keeps the return action and the local authorization context visible in every section.
@Composable
private fun ManagerHeader(shift: ShiftEntity, manager: LocalUserEntity, onReturn: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosButton("Volver a caja", onReturn)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Panel local de Manager", style = MaterialTheme.typography.titleLarge)
            Text("Tablet T1 · Turno ${shift.id.take(4).uppercase()} · ${manager.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Provides persistent navigation on landscape tablets without a crowded top bar.
@Composable
private fun ManagerSideNav(selected: ManagerSection, choose: (ManagerSection) -> Unit, modifier: Modifier) {
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ManagerSection.entries.forEach { item ->
            PosButton(item.label, { choose(item) }, selected = selected == item, modifier = Modifier.fillMaxWidth())
        }
    }
}

// Uses a compact selector in portrait so the operating area remains readable.
@Composable
private fun ManagerCompactNav(selected: ManagerSection, choose: (ManagerSection) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        PosButton("Sección: ${selected.label}", { expanded = true }, modifier = Modifier.fillMaxWidth())
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ManagerSection.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = { choose(item); expanded = false },
                )
            }
        }
    }
}

// Routes each prototype area while keeping its forms local and non-persistent.
@Composable
private fun ManagerSectionContent(
    section: ManagerSection,
    shift: ShiftEntity,
    products: List<ProductEntity>,
    pendingEvents: Int,
    modifier: Modifier,
) {
    when (section) {
        ManagerSection.Z_CLOSE -> ZClosePrototype(shift, modifier)
        ManagerSection.INVENTORY -> InventoryPrototype(products, modifier)
        ManagerSection.HISTORY -> HistoryPrototype(modifier)
        ManagerSection.STATUS -> StatusPrototype(pendingEvents, modifier)
    }
}

// Demonstrates the blind-count sequence without exposing expected cash to the cashier stage.
@Composable
private fun ZClosePrototype(shift: ShiftEntity, modifier: Modifier) {
    var stage by remember { mutableStateOf(CountStage.OPEN) }
    var count by remember { mutableStateOf("") }
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Corte Z", style = MaterialTheme.typography.headlineSmall)
            Text("Prototipo visual · las acciones no cierran ni modifican el turno.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (stage) {
                CountStage.OPEN -> {
                    Text("Turno abierto", style = MaterialTheme.typography.titleLarge)
                    Text("Cajero del turno · Fondo inicial ${Money.format(shift.openingCashCentavos)}")
                    Text("Resumen operativo: ventas por pago · mermas · descuentos · cancelaciones")
                    PosButton("Iniciar conteo ciego", { stage = CountStage.COUNTING }, primary = true)
                }
                CountStage.COUNTING -> {
                    Text("Conteo ciego · Cajero", style = MaterialTheme.typography.titleLarge)
                    Text("No se muestra efectivo esperado ni diferencia en esta etapa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Total contado: ${Money.format(Money.fromInput(count) ?: 0)}", style = MaterialTheme.typography.headlineSmall)
                    Numpad(count, { count = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PosButton("Cancelar", { stage = CountStage.OPEN }, modifier = Modifier.weight(1f))
                        PosButton("Enviar a validación", { stage = CountStage.VALIDATION }, primary = true, modifier = Modifier.weight(1f))
                    }
                }
                CountStage.VALIDATION -> {
                    val counted = Money.fromInput(count) ?: 0
                    val expected = shift.openingCashCentavos + 242_000L
                    Text("Validar corte · Manager", style = MaterialTheme.typography.titleLarge)
                    Text("Efectivo esperado: ${Money.format(expected)}")
                    Text("Contado: ${Money.format(counted)}")
                    Text("Diferencia: ${Money.format(counted - expected)}", fontWeight = FontWeight.Bold)
                    Text("El resumen y esta aprobación son demostrativos; no se generará Corte Z.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PosButton("Corregir conteo", { stage = CountStage.COUNTING }, modifier = Modifier.weight(1f))
                        PosButton("Aprobar con PIN", {}, primary = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Explores replenishment and waste controls without altering local stock or outbox records.
@Composable
private fun InventoryPrototype(products: List<ProductEntity>, modifier: Modifier) {
    var type by remember { mutableStateOf("Reposición") }
    var selected by remember { mutableStateOf(products.firstOrNull()) }
    var quantity by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Inventario operativo", style = MaterialTheme.typography.headlineSmall)
            Text("Prototipo visual · no modifica existencias ni crea eventos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosButton("Reposición", { type = "Reposición" }, selected = type == "Reposición", modifier = Modifier.weight(1f))
                PosButton("Merma", { type = "Merma" }, selected = type == "Merma", modifier = Modifier.weight(1f))
            }
            Text("Producto", style = MaterialTheme.typography.labelLarge)
            products.take(4).forEach { product ->
                PosButton(product.name, { selected = product }, selected = selected?.id == product.id, modifier = Modifier.fillMaxWidth())
            }
            Text("Cantidad: ${quantity.ifBlank { "0" }}", style = MaterialTheme.typography.titleLarge)
            Numpad(quantity, { quantity = it })
            Text("Motivo: ${if (type == "Merma") "Producto dañado" else "Reabastecimiento"}")
            PosButton("Registrar $type", { notice = "Disponible al implementar la operación persistente de Fase 2." }, primary = true)
            notice?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            HorizontalDivider()
            Text("Últimos movimientos · ejemplo", style = MaterialTheme.typography.titleMedium)
            Text("09:42 · Refresco +12 · Reabastecimiento")
            Text("10:06 · Muffin -2 · Producto dañado")
        }
    }
}

// Shows the intended ticket actions while making their non-persistent status explicit.
@Composable
private fun HistoryPrototype(modifier: Modifier) {
    var query by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Historial de esta tablet", style = MaterialTheme.typography.headlineSmall)
            Text("Prototipo visual · los tickets listados son ejemplos hasta habilitar consultas locales.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Buscar por folio") }, singleLine = true, colors = catalogFieldColors())
            TicketExample("10:22 · T1-104-0023 · $115.00 · Efectivo · Impreso", true) { notice = "La reimpresión se agregará como PrintJob en el corte funcional." }
            TicketExample("10:19 · T1-104-0022 · $76.00 · Tarjeta externa · Impreso", false) { notice = "La reimpresión se agregará como PrintJob en el corte funcional." }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

// Renders one operational history row without presenting it as an elevated card.
@Composable
private fun TicketExample(label: String, cancelable: Boolean, reprint: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PosButton("Reimprimir", reprint)
            PosButton(if (cancelable) "Cancelar" else "No cancelable en MVP", {})
        }
        HorizontalDivider()
    }
}

// Exposes only current local pending-event data; hardware values remain deliberate placeholders.
@Composable
private fun StatusPrototype(pendingEvents: Int, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Estado y periféricos", style = MaterialTheme.typography.headlineSmall)
            Text("Sincronización: $pendingEvents eventos pendientes · sin worker remoto en esta fase")
            PosButton("Intentar sincronizar ahora", {})
            HorizontalDivider()
            Text("Impresora: pendiente de hardware · prueba no disponible")
            PosButton("Imprimir prueba", {}, enabled = false)
            Text("Lector: pendiente de hardware · prueba no disponible")
            PosButton("Probar lectura", {}, enabled = false)
            Text("Estas pruebas no alteran ventas ni eliminan eventos pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

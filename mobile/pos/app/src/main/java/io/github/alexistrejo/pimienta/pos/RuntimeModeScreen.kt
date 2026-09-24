package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import kotlinx.coroutines.launch

// Shows active environment banner and allows switching mode with profile selection + Numpad verification.
@Composable
internal fun RuntimeModeBanner(
    mode: RuntimeMode,
    requiresPinForSwitch: Boolean,
    authorizers: List<LocalUserEntity> = emptyList(),
    onSwitchRequested: suspend (RuntimeMode, LocalUserEntity?, String?) -> Boolean,
    dark: Boolean = true,
    onTheme: ((Boolean) -> Unit)? = null,
    onResetDemo: (() -> Unit)? = null,
    onForceSync: (() -> Unit)? = null,
    onOpenManager: (() -> Unit)? = null,
    availableUpdateVersionName: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    var selectedAuthorizer by remember(authorizers) { mutableStateOf(authorizers.firstOrNull()) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isTraining = mode == RuntimeMode.SANDBOX
    val label = if (isTraining) "Modo Capacitación" else "Modo Venta"
    val versionLabel = "Pimienta POS · v${BuildConfig.VERSION_NAME}"

    // Top status banner bar.
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                color = if (isTraining) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                versionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            if (!availableUpdateVersionName.isNullOrBlank()) {
                Text(
                    "Hay actualización disponible (v$availableUpdateVersionName) · Panel Manager → Estado",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onOpenManager != null) {
                Button(
                    onClick = onOpenManager,
                    modifier = Modifier.heightIn(min = 36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                ) { Text("Panel Manager") }
            }
            if (onForceSync != null && !isTraining) {
                Button(
                    onClick = onForceSync,
                    modifier = Modifier.heightIn(min = 36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                ) { Text("Sincronizar") }
            }
            if (onTheme != null) {
                Button(
                    onClick = { onTheme(!dark) },
                    modifier = Modifier.heightIn(min = 36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                ) { Text(if (dark) "Tema claro" else "Tema oscuro") }
            }
            if (isTraining && onResetDemo != null) {
                Button(
                    onClick = onResetDemo,
                    modifier = Modifier.heightIn(min = 36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                ) { Text("Reiniciar datos de capacitación") }
            }
            Button(
                onClick = {
                    pin = ""
                    error = null
                    selectedAuthorizer = authorizers.firstOrNull()
                    open = true
                },
                modifier = Modifier.heightIn(min = 36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.extraSmall,
            ) { Text(if (isTraining) "Cambiar a modo venta" else "Cambiar a modo capacitación") }
        }
    }

    // Modal authorization dialog for switching modes.
    if (open) {
        val target = if (isTraining) RuntimeMode.PRODUCTION else RuntimeMode.SANDBOX
        fun submit() {
            if (requiresPinForSwitch) {
                if (selectedAuthorizer == null) {
                    error = "Selecciona un perfil de Gerente o Administrador."
                    return
                }
                if (pin.length < 4) {
                    error = "Captura los cuatro dígitos del PIN."
                    return
                }
            }
            busy = true
            scope.launch {
                val ok = onSwitchRequested(
                    target,
                    if (requiresPinForSwitch) selectedAuthorizer else null,
                    if (requiresPinForSwitch) pin else null
                )
                busy = false
                if (ok) {
                    open = false
                } else {
                    error = "PIN de ${selectedAuthorizer?.displayName ?: "Gerente o Administrador"} incorrecto."
                    pin = ""
                }
            }
        }

        Dialog(onDismissRequest = { if (!busy) open = false }) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier
                        .widthIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (isTraining) "Volver a Modo Venta" else "Cambiar a Modo Capacitación",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        if (isTraining) {
                            "Saldrás del entorno de prueba para regresar al modo de venta real."
                        } else {
                            "Entrarás al modo de capacitación con datos de prueba. Podrás practicar cobros, agregar productos de prueba y realizar cortes de caja sin afectar la información ni las ventas reales."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (requiresPinForSwitch) {
                        Text(
                            "Selecciona el autorizador (Gerente o Administrador):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (authorizers.isEmpty()) {
                            Text(
                                "No hay usuarios Gerente o Administrador activos registrados en la tablet.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                authorizers.forEach { user ->
                                    PosButton(
                                        label = user.displayTitle(isTraining),
                                        click = { selectedAuthorizer = user; error = null },
                                        selected = selectedAuthorizer?.id == user.id,
                                    )
                                }
                            }
                        }

                        Text(
                            "PIN de ${selectedAuthorizer?.displayName ?: "Gerente o Administrador"}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Numpad(pin, { pin = it; error = null }, masked = true)
                    }

                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PosButton("Cancelar", { open = false }, enabled = !busy, modifier = Modifier.weight(1f))
                        PosButton(
                            label = if (busy) "Verificando…" else if (isTraining) "Cambiar a modo venta" else "Cambiar a modo capacitación",
                            click = ::submit,
                            enabled = !busy && (!requiresPinForSwitch || (selectedAuthorizer != null && pin.length >= 4 && authorizers.isNotEmpty())),
                            primary = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

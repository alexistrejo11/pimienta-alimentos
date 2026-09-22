package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode

// Shows the active data space and gates mode changes according to build and enrollment.
@Composable
internal fun RuntimeModeBanner(
    mode: RuntimeMode,
    requiresPinForSwitch: Boolean,
    onSwitchRequested: (RuntimeMode, String?) -> Unit,
    dark: Boolean = true,
    onTheme: ((Boolean) -> Unit)? = null,
    onResetDemo: (() -> Unit)? = null,
    onForceSync: (() -> Unit)? = null,
    availableUpdateVersionName: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    val isTraining = mode == RuntimeMode.SANDBOX
    val label = if (isTraining) "Modo Capacitación" else "Modo Venta"
    val versionLabel = "Pimienta POS · v${BuildConfig.VERSION_NAME}"

    // Tight bar: sits flush above the sale StatusBar (no extra bottom gap).
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
                onClick = { pin = ""; open = true },
                modifier = Modifier.heightIn(min = 36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.extraSmall,
            ) { Text(if (isTraining) "Cambiar a modo venta" else "Cambiar a modo capacitación") }
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(if (isTraining) "Volver a Modo Venta" else "Cambiar a Modo Capacitación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isTraining) {
                            "Saldrás del entorno de prueba para regresar al modo de venta real."
                        } else {
                            "Entrarás al modo de capacitación con datos de prueba. Podrás practicar cobros, agregar productos de prueba y realizar cortes de caja sin afectar la información ni las ventas reales."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (requiresPinForSwitch) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("PIN de Gerente o Administrador") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = if (isTraining) RuntimeMode.PRODUCTION else RuntimeMode.SANDBOX
                    onSwitchRequested(target, if (requiresPinForSwitch) pin else null)
                    open = false
                }) {
                    Text(if (isTraining) "Cambiar a modo venta" else "Cambiar a modo capacitación")
                }
            },
            dismissButton = { Button(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}

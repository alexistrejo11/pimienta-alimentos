package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode

// Shows the active data space and gates mode changes according to build and enrollment.
@Composable
internal fun RuntimeModeBanner(
    mode: RuntimeMode,
    isDebug: Boolean,
    requiresPinForSwitch: Boolean,
    onSwitchRequested: (RuntimeMode, String?) -> Unit,
    onResetDemo: (() -> Unit)? = null,
    onForceSync: () -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    val label = when {
        mode == RuntimeMode.SANDBOX && isDebug -> "Playground desarrollo"
        mode == RuntimeMode.SANDBOX -> "Modo capacitaci\u00f3n"
        else -> "Modo producci\u00f3n"
    }
    // Tight bar: sits flush above the sale StatusBar (no extra bottom gap).
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onForceSync,
                modifier = Modifier.heightIn(min = 36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.extraSmall,
            ) { Text("Forzar sincronización") }
            if (mode == RuntimeMode.SANDBOX && isDebug && onResetDemo != null) {
                Button(
                    onClick = onResetDemo,
                    modifier = Modifier.heightIn(min = 36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                ) { Text("Reiniciar datos demo") }
            }
            Button(
                onClick = { pin = ""; open = true },
                modifier = Modifier.heightIn(min = 36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.extraSmall,
            ) { Text("Cambiar modo") }
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Cambiar espacio de datos") },
            text = {
                if (requiresPinForSwitch) {
                    OutlinedTextField(
                        pin,
                        { pin = it },
                        label = { Text("PIN de Manager/Superadmin") },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                } else {
                    Text(
                        if (mode == RuntimeMode.SANDBOX) {
                            "Volver a modo producci\u00f3n."
                        } else {
                            "Entrar a capacitaci\u00f3n con datos de plantilla. La base de producci\u00f3n no se modifica."
                        },
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = if (mode == RuntimeMode.SANDBOX) RuntimeMode.PRODUCTION else RuntimeMode.SANDBOX
                    onSwitchRequested(target, if (requiresPinForSwitch) pin else null)
                    open = false
                }) {
                    Text("Continuar")
                }
            },
            dismissButton = { Button(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}

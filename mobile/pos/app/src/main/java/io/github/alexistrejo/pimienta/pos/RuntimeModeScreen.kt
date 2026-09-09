package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode

// Shows the active data space and protects mode changes with a manager PIN.
@Composable
internal fun RuntimeModeBanner(mode: RuntimeMode, onSwitchRequested: (RuntimeMode, String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(if (mode == RuntimeMode.SANDBOX) "Modo capacitaci\u00f3n" else "Modo producci\u00f3n", color = MaterialTheme.colorScheme.primary)
        Button(onClick = { open = true }) { Text("Cambiar modo") }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Cambiar espacio de datos") },
            text = { OutlinedTextField(pin, { pin = it }, label = { Text("PIN de Manager/Superadmin") }, visualTransformation = PasswordVisualTransformation()) },
            confirmButton = {
                Button(onClick = { onSwitchRequested(if (mode == RuntimeMode.SANDBOX) RuntimeMode.PRODUCTION else RuntimeMode.SANDBOX, pin); open = false }) {
                    Text("Continuar")
                }
            },
            dismissButton = { Button(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}

package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Collects the one-time enrollment code without exposing technical configuration.
@Composable
internal fun EnrollmentScreen(
    busy: Boolean,
    error: String?,
    dark: Boolean = true,
    onTheme: ((Boolean) -> Unit)? = null,
    onEnroll: (code: String, name: String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("POS Android") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onTheme != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { onTheme(!dark) },
                        modifier = Modifier.heightIn(min = 36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                    ) { Text(if (dark) "☀️ Claro" else "🌙 Oscuro") }
                }
            }
            Text("Enrolar dispositivo", style = MaterialTheme.typography.headlineSmall)
            Text("Ingresa el código de un solo uso generado en la Web Central.")
            // Restrict the code to the six digits expected by the enrollment API.
            OutlinedTextField(
                value = code,
                onValueChange = { value -> code = value.filter(Char::isDigit).take(6) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código de enrolamiento") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del dispositivo") })
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            Button(
                enabled = !busy && code.length == 6,
                onClick = { onEnroll(code.trim(), name.trim()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Enrolando…" else "Enrolar dispositivo") }
        }
    }
}

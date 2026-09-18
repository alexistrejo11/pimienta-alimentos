package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Collects the one-time enrollment code without exposing technical configuration.
@Composable
internal fun EnrollmentScreen(
    busy: Boolean,
    error: String?,
    onEnroll: (code: String, name: String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("POS Android") }
    val canEnroll = !busy && code.length == 6 && name.trim().isNotEmpty()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Enrolar dispositivo", style = MaterialTheme.typography.headlineSmall)
            Text("Ingresa el código de un solo uso generado en la Web Central.")
            Text("Código de enrolamiento", style = MaterialTheme.typography.labelLarge)
            Text(
                code.ifEmpty { "------" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            // Tablet-safe digits: Compose buttons, not the system keyboard stolen by the HID wedge.
            Numpad(
                value = code,
                changed = { incoming -> code = incoming.filter(Char::isDigit).take(6) },
                masked = true,
                revealValue = true,
                onSubmit = { if (canEnroll) onEnroll(code, name.trim()) },
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre del dispositivo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                ),
                colors = catalogFieldColors(),
            )
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            PosButton(
                label = if (busy) "Enrolando…" else "Enrolar dispositivo",
                click = { onEnroll(code, name.trim()) },
                enabled = canEnroll,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

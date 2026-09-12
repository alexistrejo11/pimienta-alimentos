package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Modifier
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
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Enrolar dispositivo", style = MaterialTheme.typography.headlineSmall)
            Text("Ingresa el c\u00f3digo de un solo uso generado en la Web Central.")
            OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("C\u00f3digo de enrolamiento") })
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del dispositivo") })
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            Button(
                enabled = !busy && code.isNotBlank(),
                onClick = { onEnroll(code.trim(), name.trim()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Enrolando\u2026" else "Enrolar dispositivo") }
        }
    }
}

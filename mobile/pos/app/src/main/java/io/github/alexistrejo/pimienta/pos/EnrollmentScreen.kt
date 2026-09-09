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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

// Collects technical enrollment data without putting network work in the checkout flow.
@Composable
internal fun EnrollmentScreen(
    busy: Boolean,
    error: String?,
    onEnroll: (url: String, code: String, name: String, pin: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("POS Android") }
    var pin by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Enrolar dispositivo", style = MaterialTheme.typography.headlineSmall)
            Text("Configuraci\u00f3n t\u00e9cnica protegida. El cobro seguir\u00e1 funcionando localmente.")
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("URL del backend") })
            OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("C\u00f3digo de enrolamiento") })
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del dispositivo") })
            OutlinedTextField(pin, { pin = it }, Modifier.fillMaxWidth(), label = { Text("PIN de Manager/Superadmin") }, visualTransformation = PasswordVisualTransformation())
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            Button(
                enabled = !busy && url.isNotBlank() && code.isNotBlank() && pin.isNotBlank(),
                onClick = { onEnroll(url.trim(), code.trim(), name.trim(), pin) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Enrolando\u2026" else "Enrolar dispositivo") }
        }
    }
}

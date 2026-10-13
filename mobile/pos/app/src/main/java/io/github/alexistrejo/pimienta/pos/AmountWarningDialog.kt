package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.alexistrejo.pimienta.pos.domain.Money

// Minimal soft confirmation for an unusual amount (high total or high change); dismissing means "go back".
@Composable
internal fun AmountWarningDialog(
    title: String,
    amountCentavos: Long,
    reviewLabel: String,
    onReview: () -> Unit,
    onContinue: () -> Unit,
) {
    Dialog(onDismissRequest = onReview) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .widthIn(max = 420.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    Money.format(amountCentavos),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosButton(reviewLabel, onReview, modifier = Modifier.weight(1f))
                    PosButton("Continuar", onContinue, primary = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

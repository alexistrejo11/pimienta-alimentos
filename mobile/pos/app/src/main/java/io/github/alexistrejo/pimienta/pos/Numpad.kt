package io.github.alexistrejo.pimienta.pos

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.hardware.PosScannerRegistry
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.domain.*
import io.github.alexistrejo.pimienta.pos.hardware.PosPrinterRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PrinterFactory
import io.github.alexistrejo.pimienta.pos.hardware.printerStatusPresentation
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Keeps the typed value at a fixed height so the keys never jump on the first digit.
@Composable
private fun NumpadDisplay(
    value: String,
    decimal: Boolean,
    masked: Boolean,
    revealValue: Boolean,
) {
    val shown = when {
        decimal -> Money.format(Money.fromInput(value) ?: 0)
        masked && !revealValue -> if (value.isBlank()) "----" else "•".repeat(value.length)
        else -> value.ifEmpty { "------" }
    }
    Text(
        shown,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
    )
    Spacer(Modifier.height(4.dp))
}

// Provides number-only input without a nested scroll grid so parent screens can render cleanly.
@Composable
internal fun Numpad(
    value: String,
    changed: (String) -> Unit,
    decimal: Boolean = false,
    masked: Boolean = false,
    onSubmit: (() -> Unit)? = null,
    revealValue: Boolean = false,
    showDisplay: Boolean = true,
    keyHeight: androidx.compose.ui.unit.Dp = 38.dp,
    keySpacing: androidx.compose.ui.unit.Dp = 5.dp,
) {
    val keys = when {
        decimal -> listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "⌫")
        onSubmit != null -> listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", "⌫", "Entrar")
        else -> listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "", "0", "⌫")
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDisplay) {
            NumpadDisplay(value, decimal, masked, revealValue)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(keySpacing),
        ) {
            keys.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(keySpacing),
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f).height(keyHeight))
                        } else {
                            PosButton(
                                key,
                                {
                                    when (key) {
                                        "⌫" -> {
                                            when {
                                                value.endsWith(".00") -> changed(value.dropLast(3))
                                                value.endsWith(".0") -> changed(value.dropLast(2))
                                                else -> changed(value.dropLast(1))
                                            }
                                        }
                                        "Entrar" -> onSubmit?.invoke()
                                        "." -> if (!value.contains('.')) changed(if (value.isBlank()) "0." else "$value.")
                                        else -> {
                                            val fraction = value.substringAfter('.', missingDelimiterValue = "")
                                            val blocksExtraDecimals = value.contains('.') && fraction.length >= 2
                                            if (!blocksExtraDecimals && value.length < 8) changed(value + key)
                                        }
                                    }
                                },
                                enabled = key != "Entrar" || (onSubmit != null && value.isNotBlank()),
                                modifier = Modifier.weight(1f).height(keyHeight),
                                contentPadding = PaddingValues(0.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}


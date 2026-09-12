package io.github.alexistrejo.pimienta.pos.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.alexistrejo.pimienta.pos.R

// Bundled variable fonts matching web/src/index.html (Manrope + Work Sans).
val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
    Font(R.font.manrope, FontWeight.ExtraBold),
)

val WorkSans = FontFamily(
    Font(R.font.work_sans, FontWeight.Light),
    Font(R.font.work_sans, FontWeight.Normal),
    Font(R.font.work_sans, FontWeight.Medium),
    Font(R.font.work_sans, FontWeight.SemiBold),
)

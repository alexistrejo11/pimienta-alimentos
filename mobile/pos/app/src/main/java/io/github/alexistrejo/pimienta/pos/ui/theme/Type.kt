package io.github.alexistrejo.pimienta.pos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Headlines use Manrope; body and labels use Work Sans, same split as Angular.
val Typography = Typography(
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

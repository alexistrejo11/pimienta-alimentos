package io.github.alexistrejo.pimienta.pos.ui.theme

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryFixed,
    secondary = Accent,
    onSecondary = OnAccent,
    secondaryContainer = AccentContainer,
    onSecondaryContainer = OnAccent,
    tertiary = LightSecondary,
    onTertiary = LightOnSurface,
    error = Error,
    onError = OnPrimary,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryFixed,
    secondary = Accent,
    onSecondary = OnAccent,
    secondaryContainer = AccentContainer,
    onSecondaryContainer = OnAccent,
    tertiary = DarkSecondary,
    onTertiary = DarkOnSurface,
    error = Error,
    onError = OnPrimary,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

// Corner radii aligned with web rounded-lg / rounded-xl rather than Material card pills.
private val PosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// Compose LocalView is often a wrapper; walk to the host activity to paint system bars.
private fun Context.posActivity(): ComponentActivity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    error("PosTheme must run inside a ComponentActivity")
}

// Applies Angular tokens, paints the window (fixes white dark-mode scaffold), and fills the root surface.
@Composable
fun PosTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.posActivity()
            val scrim = colorScheme.background.toArgb()
            activity.enableEdgeToEdge(
                statusBarStyle = if (darkTheme) SystemBarStyle.dark(scrim) else SystemBarStyle.light(scrim, scrim),
                navigationBarStyle = if (darkTheme) SystemBarStyle.dark(scrim) else SystemBarStyle.light(scrim, scrim),
            )
            activity.window.decorView.setBackgroundColor(scrim)
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = PosShapes) {
        Surface(Modifier.fillMaxSize(), color = colorScheme.background, contentColor = colorScheme.onBackground, content = content)
    }
}

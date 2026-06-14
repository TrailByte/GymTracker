package org.veilon.gymtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Iron,
    onPrimary = Chalk,
    primaryContainer = IronDark,
    onPrimaryContainer = Chalk,
    secondary = Plate,
    onSecondary = Charcoal,
    background = Charcoal,
    onBackground = Chalk,
    surface = Graphite,
    onSurface = Chalk,
    surfaceVariant = GraphiteLight,
    onSurfaceVariant = Slate,
    error = Iron,
    onError = Chalk
)

private val LightColors = lightColorScheme(
    primary = Iron,
    onPrimary = Chalk,
    primaryContainer = ChalkDim,
    onPrimaryContainer = Charcoal,
    secondary = Plate,
    onSecondary = Chalk,
    background = Chalk,
    onBackground = Charcoal,
    surface = ChalkDim,
    onSurface = Charcoal,
    surfaceVariant = ChalkDim,
    onSurfaceVariant = SlateDark,
    error = Iron,
    onError = Chalk
)

@Composable
fun GymTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GymTypography,
        content = content
    )
}
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

private val ThemeSchemes: Map<String, Pair<androidx.compose.material3.ColorScheme, androidx.compose.material3.ColorScheme>> = mapOf(
    "default" to (DarkColors to LightColors),
    "bronze" to (BronzeDarkColors to BronzeLightColors),
    "steel" to (SteelDarkColors to SteelLightColors),
    "gold" to (GoldDarkColors to GoldLightColors),
    "obsidian" to (ObsidianDarkColors to ObsidianLightColors),
    "prestige" to (PrestigeDarkColors to PrestigeLightColors)
)

@Composable
fun GymTrackerTheme(
    themeMode: String = "system",
    themeId: String = "default",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    // Falls back to the default palette if themeId is somehow unrecognized —
    // e.g. an old stored value, or (shouldn't happen, but defensively) a
    // locked tier ID that slipped through.
    val (dark, light) = ThemeSchemes[themeId] ?: (DarkColors to LightColors)
    MaterialTheme(
        colorScheme = if (darkTheme) dark else light,
        typography = GymTypography,
        content = content
    )
}
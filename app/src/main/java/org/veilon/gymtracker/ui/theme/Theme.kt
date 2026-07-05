package org.veilon.gymtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

@Composable
fun GymTrackerTheme(
    themeId: String = "default",
    content: @Composable () -> Unit
) {
    // Light theme has been removed entirely — every theme, including the
    // default, is dark-only now.
    val colorScheme = when (themeId) {
        "bronze" -> BronzeDarkColors
        "steel" -> SteelDarkColors
        "gold" -> GoldDarkColors
        "obsidian" -> ObsidianDarkColors
        "prestige" -> PrestigeDarkColors
        else -> DarkColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GymTypography,
        content = content
    )
}

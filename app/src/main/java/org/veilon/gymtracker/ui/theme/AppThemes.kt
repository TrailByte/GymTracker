package org.veilon.gymtracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Each tier mirrors Iron & Chalk's exact role pattern:
// - primary/error stay the SAME vibrant accent in both dark and light mode
// - primaryContainer differs: the "Dark" accent variant in dark mode, a
//   neutral light surface in light mode (never the accent itself)
// - background/surface always neutral-toned, never muddy/warm — that was
//   the whole point of the brown-tone revision

// --- Bronze (Level 5) ---
private val BronzeAccent = Color(0xFFF2814A)
private val BronzeAccentDark = Color(0xFFC2632E)
private val BronzeDarkBg = Color(0xFF201E1C)
private val BronzeDarkSurface = Color(0xFF2B2926)
private val BronzeDarkSurfaceLight = Color(0xFF363330)
private val BronzeLightBg = Color(0xFFFAF7F4)
private val BronzeLightSurfaceDim = Color(0xFFEDE7E1)
private val BronzeMutedDark = Color(0xFFA39D96)
private val BronzeMutedLight = Color(0xFF8C8681)
private val BronzeSuccess = Color(0xFF7FAA5C)

val BronzeDarkColors: ColorScheme = darkColorScheme(
    primary = BronzeAccent, onPrimary = BronzeLightBg,
    primaryContainer = BronzeAccentDark, onPrimaryContainer = BronzeLightBg,
    secondary = BronzeSuccess, onSecondary = BronzeDarkBg,
    background = BronzeDarkBg, onBackground = BronzeLightBg,
    surface = BronzeDarkSurface, onSurface = BronzeLightBg,
    surfaceVariant = BronzeDarkSurfaceLight, onSurfaceVariant = BronzeMutedDark,
    error = BronzeAccent, onError = BronzeLightBg
)
val BronzeLightColors: ColorScheme = lightColorScheme(
    primary = BronzeAccent, onPrimary = BronzeLightBg,
    primaryContainer = BronzeLightSurfaceDim, onPrimaryContainer = BronzeDarkBg,
    secondary = BronzeSuccess, onSecondary = BronzeLightBg,
    background = BronzeLightBg, onBackground = BronzeDarkBg,
    surface = BronzeLightSurfaceDim, onSurface = BronzeDarkBg,
    surfaceVariant = BronzeLightSurfaceDim, onSurfaceVariant = BronzeMutedLight,
    error = BronzeAccent, onError = BronzeLightBg
)

// --- Steel (Level 10) ---
private val SteelAccent = Color(0xFF4FA8D8)
private val SteelAccentDark = Color(0xFF3980A8)
private val SteelDarkBg = Color(0xFF1C2024)
private val SteelDarkSurface = Color(0xFF272C30)
private val SteelDarkSurfaceLight = Color(0xFF30363B)
private val SteelLightBg = Color(0xFFEEF3F6)
private val SteelLightSurfaceDim = Color(0xFFDDE5E9)
private val SteelMutedDark = Color(0xFF8FA0AC)
private val SteelMutedLight = Color(0xFF7C8E9A)
private val SteelSuccess = Color(0xFF56B399)

val SteelDarkColors: ColorScheme = darkColorScheme(
    primary = SteelAccent, onPrimary = SteelDarkBg,
    primaryContainer = SteelAccentDark, onPrimaryContainer = SteelLightBg,
    secondary = SteelSuccess, onSecondary = SteelDarkBg,
    background = SteelDarkBg, onBackground = SteelLightBg,
    surface = SteelDarkSurface, onSurface = SteelLightBg,
    surfaceVariant = SteelDarkSurfaceLight, onSurfaceVariant = SteelMutedDark,
    error = SteelAccent, onError = SteelDarkBg
)
val SteelLightColors: ColorScheme = lightColorScheme(
    primary = SteelAccent, onPrimary = SteelDarkBg,
    primaryContainer = SteelLightSurfaceDim, onPrimaryContainer = SteelDarkBg,
    secondary = SteelSuccess, onSecondary = SteelLightBg,
    background = SteelLightBg, onBackground = SteelDarkBg,
    surface = SteelLightSurfaceDim, onSurface = SteelDarkBg,
    surfaceVariant = SteelLightSurfaceDim, onSurfaceVariant = SteelMutedLight,
    error = SteelAccent, onError = SteelDarkBg
)

// --- Gold (Level 20) ---
private val GoldAccent = Color(0xFFF0B93D)
private val GoldAccentDark = Color(0xFFC4930F)
private val GoldDarkBg = Color(0xFF1F1F1D)
private val GoldDarkSurface = Color(0xFF2A2A26)
private val GoldDarkSurfaceLight = Color(0xFF34342F)
private val GoldLightBg = Color(0xFFFAF6EA)
private val GoldLightSurfaceDim = Color(0xFFEDE6D0)
private val GoldMutedDark = Color(0xFFB3A57E)
private val GoldMutedLight = Color(0xFF9C8E68)
private val GoldSuccess = Color(0xFF8FC24E)

val GoldDarkColors: ColorScheme = darkColorScheme(
    primary = GoldAccent, onPrimary = GoldDarkBg,
    primaryContainer = GoldAccentDark, onPrimaryContainer = GoldDarkBg,
    secondary = GoldSuccess, onSecondary = GoldDarkBg,
    background = GoldDarkBg, onBackground = GoldLightBg,
    surface = GoldDarkSurface, onSurface = GoldLightBg,
    surfaceVariant = GoldDarkSurfaceLight, onSurfaceVariant = GoldMutedDark,
    error = GoldAccent, onError = GoldDarkBg
)
val GoldLightColors: ColorScheme = lightColorScheme(
    primary = GoldAccentDark, onPrimary = GoldLightBg,
    primaryContainer = GoldLightSurfaceDim, onPrimaryContainer = GoldDarkBg,
    secondary = GoldSuccess, onSecondary = GoldLightBg,
    background = GoldLightBg, onBackground = GoldDarkBg,
    surface = GoldLightSurfaceDim, onSurface = GoldDarkBg,
    surfaceVariant = GoldLightSurfaceDim, onSurfaceVariant = GoldMutedLight,
    error = GoldAccentDark, onError = GoldLightBg
)

// --- Obsidian (Level 30, season cap) ---
private val ObsidianAccent = Color(0xFFA78BFA)
private val ObsidianAccentDark = Color(0xFF7C5FD1)
private val ObsidianDarkBg = Color(0xFF242229)
private val ObsidianDarkSurface = Color(0xFF2F2D36)
private val ObsidianDarkSurfaceLight = Color(0xFF3A3742)
private val ObsidianLightBg = Color(0xFFF1EEF7)
private val ObsidianLightSurfaceDim = Color(0xFFE2DCEF)
private val ObsidianMutedDark = Color(0xFFA79FBD)
private val ObsidianMutedLight = Color(0xFF8E85A3)
private val ObsidianSuccess = Color(0xFF6FCF97)

val ObsidianDarkColors: ColorScheme = darkColorScheme(
    primary = ObsidianAccent, onPrimary = ObsidianDarkBg,
    primaryContainer = ObsidianAccentDark, onPrimaryContainer = ObsidianLightBg,
    secondary = ObsidianSuccess, onSecondary = ObsidianDarkBg,
    background = ObsidianDarkBg, onBackground = ObsidianLightBg,
    surface = ObsidianDarkSurface, onSurface = ObsidianLightBg,
    surfaceVariant = ObsidianDarkSurfaceLight, onSurfaceVariant = ObsidianMutedDark,
    error = ObsidianAccent, onError = ObsidianDarkBg
)
val ObsidianLightColors: ColorScheme = lightColorScheme(
    primary = ObsidianAccentDark, onPrimary = ObsidianLightBg,
    primaryContainer = ObsidianLightSurfaceDim, onPrimaryContainer = ObsidianDarkBg,
    secondary = ObsidianSuccess, onSecondary = ObsidianLightBg,
    background = ObsidianLightBg, onBackground = ObsidianDarkBg,
    surface = ObsidianLightSurfaceDim, onSurface = ObsidianDarkBg,
    surfaceVariant = ObsidianLightSurfaceDim, onSurfaceVariant = ObsidianMutedLight,
    error = ObsidianAccentDark, onError = ObsidianLightBg
)

// --- Prestige (unlocked via prestiging, not the level ladder) ---
private val PrestigeAccent = Color(0xFFFFC845)
private val PrestigeAccentDark = Color(0xFFD9A017)
private val PrestigeDarkBg = Color(0xFF201E1A)
private val PrestigeDarkSurface = Color(0xFF2B2925)
private val PrestigeDarkSurfaceLight = Color(0xFF35322D)
private val PrestigeLightBg = Color(0xFFFBF6E8)
private val PrestigeLightSurfaceDim = Color(0xFFF0E7C8)
private val PrestigeMutedDark = Color(0xFFC4AC7A)
private val PrestigeMutedLight = Color(0xFFA88F5C)
private val PrestigeSuccess = Color(0xFF7FC25C)

val PrestigeDarkColors: ColorScheme = darkColorScheme(
    primary = PrestigeAccent, onPrimary = PrestigeDarkBg,
    primaryContainer = PrestigeAccentDark, onPrimaryContainer = PrestigeDarkBg,
    secondary = PrestigeSuccess, onSecondary = PrestigeDarkBg,
    background = PrestigeDarkBg, onBackground = PrestigeLightBg,
    surface = PrestigeDarkSurface, onSurface = PrestigeLightBg,
    surfaceVariant = PrestigeDarkSurfaceLight, onSurfaceVariant = PrestigeMutedDark,
    error = PrestigeAccent, onError = PrestigeDarkBg
)
val PrestigeLightColors: ColorScheme = lightColorScheme(
    primary = PrestigeAccentDark, onPrimary = PrestigeLightBg,
    primaryContainer = PrestigeLightSurfaceDim, onPrimaryContainer = PrestigeDarkBg,
    secondary = PrestigeSuccess, onSecondary = PrestigeLightBg,
    background = PrestigeLightBg, onBackground = PrestigeDarkBg,
    surface = PrestigeLightSurfaceDim, onSurface = PrestigeDarkBg,
    surfaceVariant = PrestigeLightSurfaceDim, onSurfaceVariant = PrestigeMutedLight,
    error = PrestigeAccentDark, onError = PrestigeLightBg
)

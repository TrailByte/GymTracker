package org.veilon.gymtracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Dark-only: light mode was removed for these tiers. onPrimary/onError/
// onSecondary all use dark text — verified by actual WCAG contrast math,
// since these accent hues are noticeably lighter than Iron, and light text
// on them measured as low as 1.5:1 (objectively hard to read).

// --- Bronze (Level 5) ---
private val BronzeAccent = Color(0xFFF2814A)
private val BronzeAccentDark = Color(0xFFC2632E)
private val BronzeDarkBg = Color(0xFF201E1C)
private val BronzeDarkSurface = Color(0xFF2B2926)
private val BronzeDarkSurfaceLight = Color(0xFF363330)
private val BronzeLightBg = Color(0xFFFAF7F4)
private val BronzeMutedDark = Color(0xFFA39D96)
private val BronzeSuccess = Color(0xFF7FAA5C)

val BronzeDarkColors: ColorScheme = darkColorScheme(
    primary = BronzeAccent, onPrimary = BronzeDarkBg,
    primaryContainer = BronzeAccentDark, onPrimaryContainer = BronzeLightBg,
    secondary = BronzeSuccess, onSecondary = BronzeDarkBg,
    background = BronzeDarkBg, onBackground = BronzeLightBg,
    surface = BronzeDarkSurface, onSurface = BronzeLightBg,
    surfaceVariant = BronzeDarkSurfaceLight, onSurfaceVariant = BronzeMutedDark,
    error = BronzeAccent, onError = BronzeDarkBg
)

// --- Steel (Level 10) ---
private val SteelAccent = Color(0xFF4FA8D8)
private val SteelAccentDark = Color(0xFF3980A8)
private val SteelDarkBg = Color(0xFF1C2024)
private val SteelDarkSurface = Color(0xFF272C30)
private val SteelDarkSurfaceLight = Color(0xFF30363B)
private val SteelLightBg = Color(0xFFEEF3F6)
private val SteelMutedDark = Color(0xFF8FA0AC)
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

// --- Gold (Level 20) ---
private val GoldAccent = Color(0xFFF0B93D)
private val GoldAccentDark = Color(0xFFC4930F)
private val GoldDarkBg = Color(0xFF1F1F1D)
private val GoldDarkSurface = Color(0xFF2A2A26)
private val GoldDarkSurfaceLight = Color(0xFF34342F)
private val GoldLightBg = Color(0xFFFAF6EA)
private val GoldMutedDark = Color(0xFFB3A57E)
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

// --- Obsidian (Level 30, season cap) ---
private val ObsidianAccent = Color(0xFFA78BFA)
private val ObsidianAccentDark = Color(0xFF7C5FD1)
private val ObsidianDarkBg = Color(0xFF242229)
private val ObsidianDarkSurface = Color(0xFF2F2D36)
private val ObsidianDarkSurfaceLight = Color(0xFF3A3742)
private val ObsidianLightBg = Color(0xFFF1EEF7)
private val ObsidianMutedDark = Color(0xFFA79FBD)
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

// --- Prestige (unlocked via prestiging, not the level ladder) ---
private val PrestigeAccent = Color(0xFFFFC845)
private val PrestigeAccentDark = Color(0xFFD9A017)
private val PrestigeDarkBg = Color(0xFF201E1A)
private val PrestigeDarkSurface = Color(0xFF2B2925)
private val PrestigeDarkSurfaceLight = Color(0xFF35322D)
private val PrestigeLightBg = Color(0xFFFBF6E8)
private val PrestigeMutedDark = Color(0xFFC4AC7A)
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

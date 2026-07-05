package org.veilon.gymtracker.ui.theme

data class ThemeTier(val id: String, val displayName: String, val unlockLevel: Int)

object ThemeUnlocks {
    val LADDER = listOf(
        ThemeTier("default", "Iron & Chalk", 1),
        ThemeTier("bronze", "Bronze", 5),
        ThemeTier("steel", "Steel", 10),
        ThemeTier("gold", "Gold", 20),
        ThemeTier("obsidian", "Obsidian", 30)
    )

    // The Prestige theme isn't on the level ladder — it unlocks via prestiging,
    // checked separately (prestigeLevel > 0), not by comparing to a level number.
    val PRESTIGE_THEME = ThemeTier("prestige", "Prestige", unlockLevel = 0)

    fun themeUnlockedAtLevel(level: Int): ThemeTier? = LADDER.find { it.unlockLevel == level }

    fun unlockedThemes(level: Int, prestige: Int): List<ThemeTier> {
        val fromLevel = LADDER.filter { it.unlockLevel <= level }
        return if (prestige > 0) fromLevel + PRESTIGE_THEME else fromLevel
    }
}

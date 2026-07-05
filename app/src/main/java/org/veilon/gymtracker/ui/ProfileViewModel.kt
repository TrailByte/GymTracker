package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.veilon.gymtracker.gamification.GamificationEngine

data class ProfileStats(
    val level: Int,
    val prestige: Int,
    val xpIntoLevel: Long,
    val xpForLevel: Long,
    val isMaxLevel: Boolean
)

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    val profileStats = combine(
        UserPreferences.totalXp(app),
        UserPreferences.prestigeLevel(app)
    ) { xp, prestige ->
        val progress = GamificationEngine.progressWithinLevel(xp)
        if (progress == null) {
            ProfileStats(
                level = GamificationEngine.MAX_SEASON_LEVEL,
                prestige = prestige,
                xpIntoLevel = 0,
                xpForLevel = 0,
                isMaxLevel = true
            )
        } else {
            val (into, needed, level) = progress
            ProfileStats(
                level = level,
                prestige = prestige,
                xpIntoLevel = into,
                xpForLevel = needed,
                isMaxLevel = false
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProfileStats(level = 1, prestige = 0, xpIntoLevel = 0, xpForLevel = 100, isMaxLevel = false)
    )
}

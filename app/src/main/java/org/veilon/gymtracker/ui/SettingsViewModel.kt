package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.veilon.gymtracker.gamification.GamificationEngine

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext

    val useLbs = UserPreferences.useLbs(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val restSeconds = UserPreferences.restSeconds(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 90)

    fun setUseLbs(value: Boolean) {
        viewModelScope.launch { UserPreferences.setUseLbs(appContext, value) }
    }

    fun setRestSeconds(value: Int) {
        viewModelScope.launch {
            UserPreferences.setRestSeconds(appContext, value.coerceAtLeast(0))
        }
    }

    val weeklyGoal = UserPreferences.weeklyGoal(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    fun setWeeklyGoal(value: Int) {
        viewModelScope.launch { UserPreferences.setWeeklyGoal(appContext, value) }
    }

    val themeMode = UserPreferences.themeMode(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun setThemeMode(mode: String) {
        viewModelScope.launch { UserPreferences.setThemeMode(appContext, mode) }
    }

    val selectedTheme = UserPreferences.selectedTheme(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "default")

    fun setSelectedTheme(themeId: String) {
        viewModelScope.launch { UserPreferences.setSelectedTheme(appContext, themeId) }
    }

    // Level + prestige, so the theme picker knows which tiers are unlocked
    val levelAndPrestige = combine(
        UserPreferences.totalXp(app),
        UserPreferences.prestigeLevel(app)
    ) { xp, prestige -> GamificationEngine.levelForXp(xp) to prestige }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1 to 0)
}
package org.veilon.gymtracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.veilon.gymtracker.data.BackupManager
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

    // Backup / restore
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val success = BackupManager.exportBackup(appContext, uri)
            _backupStatus.value = if (success) {
                "Backup saved."
            } else {
                "Backup failed — please try again."
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val success = BackupManager.importBackup(appContext, uri)
            _backupStatus.value = if (success) {
                "Import complete. Please close and reopen the app now."
            } else {
                "Import failed — the file may not be a valid backup."
            }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }
}
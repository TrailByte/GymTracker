package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.gamification.AchievementDef
import org.veilon.gymtracker.gamification.Achievements

data class AchievementRow(
    val def: AchievementDef,
    val unlocked: Boolean,
    val unlockedDate: Long?
)

class AchievementsViewModel(app: Application) : AndroidViewModel(app) {

    private val achievementDao = AppDatabase.getInstance(app).achievementDao()

    val rows = achievementDao.getAllUnlocked().map { unlockedList ->
        val dateById = unlockedList.associate { it.achievementId to it.unlockedDate }
        Achievements.ALL.map { def ->
            AchievementRow(
                def = def,
                unlocked = def.id in dateById,
                unlockedDate = dateById[def.id]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

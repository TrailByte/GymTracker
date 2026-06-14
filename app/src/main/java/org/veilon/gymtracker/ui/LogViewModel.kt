package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.WorkoutSession

data class LogState(
    val activeSession: WorkoutSession?,
    val history: List<WorkoutSession>
)

class LogViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()

    val state = combine(
        workoutDao.getAllSessions(),
        UserPreferences.activeSession(app)
    ) { sessions, activeId ->
        val active = sessions.find { it.id == activeId }
        val history = sessions.filter { it.id != activeId }
        LogState(active, history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogState(null, emptyList()))
}
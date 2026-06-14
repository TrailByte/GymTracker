package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.veilon.gymtracker.data.AppDatabase

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()

    // Recent finished workouts = all sessions except the currently-active one
    val recentSessions = combine(
        workoutDao.getAllSessions(),
        UserPreferences.activeSession(app)
    ) { sessions, activeId ->
        sessions.filter { it.id != activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.WorkoutSession

class ActiveWorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext
    private val workoutDao = AppDatabase.getInstance(app).workoutDao()

    val activeSessionId = UserPreferences.activeSession(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Create a new empty session, mark it active, return its id via callback
    fun startEmpty(name: String, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val id = workoutDao.insertSession(WorkoutSession(name = name))
            UserPreferences.setActiveSession(appContext, id)
            onStarted(id)
        }
    }

    fun clearActive() {
        viewModelScope.launch {
            UserPreferences.setActiveSession(appContext, null)
        }
    }
}
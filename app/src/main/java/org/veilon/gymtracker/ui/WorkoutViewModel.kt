package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.Exercise
import org.veilon.gymtracker.data.ExerciseLog

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()

    val exercises = exerciseDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow<Int?>(null)
    val restTimerSeconds = _restTimerSeconds.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    fun setSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun getLogsForSession(sessionId: Long) =
        workoutDao.getLogsForSession(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logSet(sessionId: Long, exercise: Exercise, setNumber: Int, reps: Int, weight: Double) {
        viewModelScope.launch {
            workoutDao.insertLog(
                ExerciseLog(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    setNumber = setNumber,
                    reps = reps,
                    weight = weight
                )
            )
            _restTimerSeconds.value = 90 // default 90s rest
        }
    }

    fun tickRestTimer() {
        val current = _restTimerSeconds.value ?: return
        _restTimerSeconds.value = if (current <= 1) null else current - 1
    }

    fun addRestTime(seconds: Int) {
        _restTimerSeconds.value = (_restTimerSeconds.value ?: 0) + seconds
    }

    fun skipRest() {
        _restTimerSeconds.value = null
    }

    fun tickElapsed() {
        _elapsedSeconds.value += 1
    }
}
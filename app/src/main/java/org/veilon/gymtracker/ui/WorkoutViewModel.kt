package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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

    // Weight unit preference (true = lbs, false = kg)
    val useLbs = UserPreferences.useLbs(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _currentSessionId = MutableStateFlow<Long?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val logs: StateFlow<List<ExerciseLog>> = _currentSessionId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else workoutDao.getLogsForSession(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _restTimerSeconds = MutableStateFlow<Int?>(null)
    val restTimerSeconds = _restTimerSeconds.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    fun setSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    // Add a new empty set for an exercise (weight stored in kg)
    fun addSet(sessionId: Long, exercise: Exercise) {
        viewModelScope.launch {
            val nextSet = logs.value.count { it.exerciseId == exercise.id } + 1
            workoutDao.insertLog(
                ExerciseLog(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    setNumber = nextSet,
                    reps = 0,
                    weight = 0.0,
                    completed = false
                )
            )
        }
    }

    // Edit reps/weight inline. weightKg is already converted to kg by the UI.
    fun updateSet(log: ExerciseLog, reps: Int, weightKg: Double) {
        viewModelScope.launch {
            workoutDao.updateLog(log.copy(reps = reps, weight = weightKg))
        }
    }

    // Toggle the completed checkmark; starting rest only when marking ON
    fun toggleComplete(log: ExerciseLog) {
        viewModelScope.launch {
            val nowComplete = !log.completed
            workoutDao.updateLog(log.copy(completed = nowComplete))
            if (nowComplete) {
                _restTimerSeconds.value = 90
            }
        }
    }

    fun deleteSet(log: ExerciseLog) {
        viewModelScope.launch { workoutDao.deleteLog(log) }
    }

    fun deleteExercise(sessionId: Long, exerciseId: Long) {
        viewModelScope.launch {
            workoutDao.deleteExerciseFromSession(sessionId, exerciseId)
        }
    }

    // Finish: optionally discard any sets the user never marked complete
    fun finishWorkout(sessionId: Long, discardIncomplete: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            if (discardIncomplete) {
                workoutDao.deleteIncompleteLogs(sessionId)
            } else {
                // Mark everything complete
                logs.value.filter { !it.completed }.forEach {
                    workoutDao.updateLog(it.copy(completed = true))
                }
            }
            _restTimerSeconds.value = null
            onDone()
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
package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.Exercise
import org.veilon.gymtracker.data.ExerciseLog
import org.veilon.gymtracker.data.WorkoutSession

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()
    private val appContext = app.applicationContext

    val exercises = exerciseDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Per-session rest duration, initialized from the global default
    private val _restDuration = MutableStateFlow(90)
    val restDuration = _restDuration.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    init {
        // Seed the per-session duration from the saved global default
        viewModelScope.launch {
            _restDuration.value = UserPreferences.restSeconds(appContext).first()
        }
    }

    fun setSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun setRestDuration(seconds: Int) {
        _restDuration.value = seconds.coerceAtLeast(0)
    }

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

    fun updateSet(log: ExerciseLog, reps: Int, weightKg: Double) {
        viewModelScope.launch {
            workoutDao.updateLog(log.copy(reps = reps, weight = weightKg))
        }
    }

    fun toggleComplete(log: ExerciseLog) {
        viewModelScope.launch {
            val nowComplete = !log.completed
            workoutDao.updateLog(log.copy(completed = nowComplete))
            if (nowComplete) {
                _restTimerSeconds.value = _restDuration.value
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

    fun finishWorkout(sessionId: Long, discardIncomplete: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            if (discardIncomplete) {
                workoutDao.deleteIncompleteLogs(sessionId)
            } else {
                logs.value.filter { !it.completed }.forEach {
                    workoutDao.updateLog(it.copy(completed = true))
                }
            }
            _restTimerSeconds.value = null
            onDone()
        }
    }

    // Cancel: delete the whole session (CASCADE removes its logs)
    fun cancelWorkout(sessionId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            workoutDao.deleteSession(WorkoutSession(id = sessionId, name = "", date = 0))
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
package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import org.veilon.gymtracker.RestTimerService
import org.veilon.gymtracker.RestAlerts

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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessionName: StateFlow<String> = _currentSessionId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf("Workout")
            else kotlinx.coroutines.flow.flowOf(workoutDao.getSession(id)?.name ?: "Workout")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Workout")

    // Rest timer is timestamp-based: store WHEN rest ends; compute remaining live.
    // Lives in DataStore so the mini-bar (different ViewModel) reads the same value
    // and it survives navigation / minimize / backgrounding.
    val restEndsAt = UserPreferences.restEndsAt(appContext)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _restForExerciseId = MutableStateFlow<Long?>(null)
    val restForExerciseId = _restForExerciseId.asStateFlow()

    // Per-session rest duration, initialized from the global default
    private val _restDuration = MutableStateFlow(90)
    val restDuration = _restDuration.asStateFlow()

    // Workout start time (epoch millis); elapsed is computed from this, not counted
    private val _startTime = MutableStateFlow<Long?>(null)
    val startTime = _startTime.asStateFlow()

    // In-app rest alert timer (primary path while the app is open)
    private var restAlertJob: Job? = null

    init {
        viewModelScope.launch {
            _restDuration.value = UserPreferences.restSeconds(appContext).first()
        }
    }

    fun setSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            _startTime.value = workoutDao.getSession(sessionId)?.date
        }
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
                val endsAt = System.currentTimeMillis() + _restDuration.value * 1000L
                UserPreferences.setRestEndsAt(appContext, endsAt)
                _restForExerciseId.value = log.exerciseId
                RestTimerService.start(appContext, endsAt)
                startRestAlertTimer(endsAt)
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
            val session = workoutDao.getSession(sessionId)
            if (session != null) {
                val durationSec = ((System.currentTimeMillis() - session.date) / 1000).coerceAtLeast(0)
                workoutDao.updateSession(session.copy(durationSeconds = durationSec))
            }
            UserPreferences.setRestEndsAt(appContext, null)
            RestTimerService.stop(appContext)
            cancelRestAlertTimer()
            _restForExerciseId.value = null
            onDone()
        }
    }

    // Cancel: delete the whole session (CASCADE removes its logs)
    fun cancelWorkout(sessionId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            workoutDao.deleteSession(WorkoutSession(id = sessionId, name = "", date = 0))
            UserPreferences.setRestEndsAt(appContext, null)
            RestTimerService.stop(appContext)
            cancelRestAlertTimer()
            _restForExerciseId.value = null
            onDone()
        }
    }

    fun addRestTime(seconds: Int) {
        viewModelScope.launch {
            val current = restEndsAt.value ?: System.currentTimeMillis()
            val newEnd = current + seconds * 1000L
            UserPreferences.setRestEndsAt(appContext, newEnd)
            RestTimerService.start(appContext, newEnd)
            startRestAlertTimer(newEnd)
        }
    }

    fun skipRest() {
        viewModelScope.launch {
            UserPreferences.setRestEndsAt(appContext, null)
            _restForExerciseId.value = null
            RestTimerService.stop(appContext)
            cancelRestAlertTimer()
        }
    }

    // In-app alert timer — primary path while the app is open.
    // Buzzes at 3-2-1, fires finish alert at zero, then clears rest state
    // (which signals the alarm backup that it's already handled).
    private fun startRestAlertTimer(endsAt: Long) {
        restAlertJob?.cancel()
        restAlertJob = viewModelScope.launch {
            val buzzedAt = mutableSetOf<Int>()
            while (true) {
                val msLeft = endsAt - System.currentTimeMillis()
                if (msLeft <= 0) {
                    RestAlerts.finishAlert(appContext)
                    UserPreferences.setRestEndsAt(appContext, null)
                    _restForExerciseId.value = null
                    RestTimerService.stop(appContext)
                    break
                }
                val secLeft = Math.ceil(msLeft / 1000.0).toInt()
                if (secLeft in 1..3 && secLeft !in buzzedAt) {
                    buzzedAt.add(secLeft)
                    RestAlerts.buzz(appContext)
                }
                delay(100)
            }
        }
    }

    private fun cancelRestAlertTimer() {
        restAlertJob?.cancel()
        restAlertJob = null
    }
}

package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.ExerciseLog
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import org.veilon.gymtracker.data.SessionExerciseOrder

class SessionDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()

    val exercises = exerciseDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val useLbs = UserPreferences.useLbs(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _sessionId = MutableStateFlow<Long?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val logs: StateFlow<List<ExerciseLog>> = _sessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else workoutDao.getLogsForSession(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val savedOrder: StateFlow<List<SessionExerciseOrder>> = _sessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else workoutDao.getExerciseOrder(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orderedExerciseIds: StateFlow<List<Long>> = combine(logs, savedOrder) { logsList, orderList ->
        val loggedIds = logsList.map { it.exerciseId }.distinct()
        val explicit = orderList.sortedBy { it.orderIndex }
            .map { it.exerciseId }
            .filter { it in loggedIds }
        val missing = loggedIds.filter { it !in explicit }
        explicit + missing
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSession(sessionId: Long) {
        _sessionId.value = sessionId
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessionName: StateFlow<String> = _sessionId
        .flatMapLatest { id ->
            if (id == null) flowOf("Workout")
            else flowOf(workoutDao.getSession(id)?.name ?: "Workout")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Workout")

    fun deleteSession(onDone: () -> Unit) {
        val id = _sessionId.value ?: return
        viewModelScope.launch {
            workoutDao.getSession(id)?.let { workoutDao.deleteSession(it) }
            onDone()
        }
    }
}
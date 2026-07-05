package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.veilon.gymtracker.data.AppDatabase

data class ExerciseHistory(
    val weightPoints: List<Pair<Long, Double>>,  // (sessionDate, best weight that session)
    val volumePoints: List<Pair<Long, Double>>   // (sessionDate, total volume that session)
)

class ExerciseHistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()

    val useLbs = UserPreferences.useLbs(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Documented project pattern: id lives in a MutableStateFlow, driven by
    // setExerciseId() from a LaunchedEffect in the screen, with flatMapLatest
    // downstream. Building a fresh flow inside a function the composable calls
    // directly (instead of this) is the known flicker trap — avoided here.
    private val _exerciseId = MutableStateFlow<Long?>(null)

    fun setExerciseId(id: Long) {
        _exerciseId.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val exerciseName: StateFlow<String> = _exerciseId.flatMapLatest { id ->
        if (id == null) flowOf("")
        else exerciseDao.getAllIncludingArchived().map { list -> list.find { it.id == id }?.name ?: "" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val logsForExercise = _exerciseId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else workoutDao.getCompletedLogsForExercise(id)
    }

    val history: StateFlow<ExerciseHistory> = combine(
        logsForExercise,
        workoutDao.getAllSessions()
    ) { logs, sessions ->
        val dateBySession = sessions.associate { it.id to it.date }
        val bySession = logs.groupBy { it.sessionId }
        val points = bySession.mapNotNull { (sessionId, sLogs) ->
            val date = dateBySession[sessionId] ?: return@mapNotNull null
            val bestWeight = sLogs.maxOf { it.weight }
            val totalVolume = sLogs.sumOf { it.weight * it.reps }
            Triple(date, bestWeight, totalVolume)
        }.sortedBy { it.first }
        ExerciseHistory(
            weightPoints = points.map { it.first to it.second },
            volumePoints = points.map { it.first to it.third }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseHistory(emptyList(), emptyList()))
}

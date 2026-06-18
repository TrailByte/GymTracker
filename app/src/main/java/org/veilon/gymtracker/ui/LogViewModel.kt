package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.WorkoutSession
import kotlinx.coroutines.launch

data class ExerciseLine(val name: String, val setCount: Int)

data class SessionStats(
    val exercises: List<ExerciseLine>,
    val setCount: Int,
    val totalVolumeKg: Double
)

data class LogState(
    val activeSession: WorkoutSession?,
    val history: List<WorkoutSession>,
    val statsBySession: Map<Long, SessionStats>
)

class LogViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()

    val useLbs = UserPreferences.useLbs(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val state = combine(
        workoutDao.getAllSessions(),
        workoutDao.getAllLogs(),
        exerciseDao.getAllIncludingArchived(),
        UserPreferences.activeSession(app)
    ) { sessions, allLogs, exercises, activeId ->
        val active = sessions.find { it.id == activeId }
        val history = sessions.filter { it.id != activeId }

        val nameById = exercises.associate { it.id to it.name }
        val logsBySession = allLogs.groupBy { it.sessionId }

        val stats = logsBySession.mapValues { (_, logs) ->
            // Preserve workout order = order exercises first appear in the logs
            val orderedIds = logs.map { it.exerciseId }.distinct()
            val lines = orderedIds.map { exId ->
                ExerciseLine(
                    name = nameById[exId] ?: "Unknown",
                    setCount = logs.count { it.exerciseId == exId }
                )
            }
            SessionStats(
                exercises = lines,
                setCount = logs.size,
                totalVolumeKg = logs.sumOf { it.weight * it.reps }
            )
        }
        LogState(active, history, stats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogState(null, emptyList(), emptyMap()))

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch {
            workoutDao.deleteSession(session)
        }
    }
}
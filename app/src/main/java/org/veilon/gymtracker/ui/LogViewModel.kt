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

data class ExerciseLine(val name: String, val equipmentType: String, val setCount: Int)

data class SessionStats(
    val exercises: List<ExerciseLine>,
    val setCount: Int,
    val totalVolumeKg: Double,
    val hasPR: Boolean
)

data class LogState(
    val activeSession: WorkoutSession?,
    val history: List<WorkoutSession>,
    val statsBySession: Map<Long, SessionStats>
)

class LogViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()
    private val recordDao = AppDatabase.getInstance(app).recordDao()

    val useLbs = UserPreferences.useLbs(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val orderAndRecords = combine(
        workoutDao.getAllExerciseOrder(),
        recordDao.getAllRecords()
    ) { allOrder, records -> allOrder to records }

    val state = combine(
        workoutDao.getAllSessions(),
        workoutDao.getAllLogs(),
        exerciseDao.getAllIncludingArchived(),
        UserPreferences.activeSession(app),
        orderAndRecords
    ) { sessions, allLogs, exercises, activeId, (allOrder, records) ->
        val active = sessions.find { it.id == activeId }
        val history = sessions.filter { it.id != activeId }

        val nameById = exercises.associate { it.id to it.name }
        val equipmentById = exercises.associate { it.id to it.equipmentType }
        val logsBySession = allLogs.groupBy { it.sessionId }
        val orderBySession = allOrder.groupBy { it.sessionId }
        val sessionById = sessions.associateBy { it.id }

        val stats = logsBySession.mapValues { (sessionId, logs) ->
            // Use the saved drag-order if one exists; otherwise fall back to the
            // order exercises were first logged (unchanged behavior for old workouts).
            val loggedIds = logs.map { it.exerciseId }.distinct()
            val explicit = orderBySession[sessionId].orEmpty()
                .sortedBy { it.orderIndex }
                .map { it.exerciseId }
                .filter { it in loggedIds }
            val orderedIds = explicit + loggedIds.filter { it !in explicit }

            val lines = orderedIds.map { exId ->
                ExerciseLine(
                    name = nameById[exId] ?: "Unknown",
                    equipmentType = equipmentById[exId] ?: "",
                    setCount = logs.count { it.exerciseId == exId }
                )
            }

            val session = sessionById[sessionId]
            val sessionStart = session?.date ?: 0L
            val sessionEnd = sessionStart + (session?.durationSeconds ?: 0L) * 1000L
            val prExercises = exercisesPrdInSession(
                logs.filter { it.completed },
                sessionStart,
                sessionEnd,
                records
            )

            SessionStats(
                exercises = lines,
                setCount = logs.size,
                totalVolumeKg = logs.sumOf { it.weight * it.reps },
                hasPR = prExercises.isNotEmpty()
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
package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.Exercise
import org.veilon.gymtracker.data.ExerciseLog
import org.veilon.gymtracker.data.WorkoutSession
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.veilon.gymtracker.data.ExerciseRecord
import org.veilon.gymtracker.ui.computeWeekStreak
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val templateDao = AppDatabase.getInstance(app).templateDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()
    private val recordDao = AppDatabase.getInstance(app).recordDao()

    init {
        viewModelScope.launch {
            if (!UserPreferences.recordsBackfilled(app).first()) {
                val allLogs = workoutDao.getAllCompletedLogsOnce()
                val dateBySession = workoutDao.getAllSessions().first().associate { it.id to it.date }
                allLogs.groupBy { it.exerciseId }.forEach { (exerciseId, exLogs) ->
                    val maxWeightLog = exLogs.maxByOrNull { it.weight }
                    val maxVolLog = exLogs.maxByOrNull { it.weight * it.reps }
                    if (maxWeightLog != null && maxVolLog != null) {
                        recordDao.upsertRecord(
                            ExerciseRecord(
                                exerciseId = exerciseId,
                                maxWeightKg = maxWeightLog.weight,
                                maxWeightReps = maxWeightLog.reps,
                                maxWeightDate = dateBySession[maxWeightLog.sessionId] ?: System.currentTimeMillis(),
                                maxVolumeKg = maxVolLog.weight * maxVolLog.reps,
                                maxVolumeWeightKg = maxVolLog.weight,
                                maxVolumeReps = maxVolLog.reps,
                                maxVolumeDate = dateBySession[maxVolLog.sessionId] ?: System.currentTimeMillis()
                            )
                        )
                    }
                }
                UserPreferences.setRecordsBackfilled(app, true)
            }
        }
    }

    val useLbs = UserPreferences.useLbs(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recentSessions = combine(
        workoutDao.getAllSessions(),
        UserPreferences.activeSession(app)
    ) { sessions, activeId ->
        sessions.filter { it.id != activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates = templateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combine sessions + completed logs + exercises + goal into one stats object
    val stats = combine(
        workoutDao.getAllSessions(),
        workoutDao.getAllCompletedLogs(),
        exerciseDao.getAll(),
        UserPreferences.weeklyGoal(app),
        recordDao.getAllRecords()
    ) { sessions, logs, exercises, goal, records ->
        computeStats(sessions, logs, exercises, goal, records)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        HomeStats(0, 0, 0.0, emptyList())
    )

    private fun computeStats(
        sessions: List<WorkoutSession>,
        logs: List<ExerciseLog>,
        exercises: List<Exercise>,
        weeklyGoal: Int,
        records: List<ExerciseRecord>
    ): HomeStats {
        val workoutsThisWeek = sessions.count { isThisWeek(it.date) }
        val totalVolume = logs.sumOf { it.weight * it.reps }
        val streak = computeWeekStreak(sessions.map { it.date }, weeklyGoal)

        // PRs now come from the stored records table — O(1) per exercise,
        // not a scan across every completed set ever logged.
        val exerciseById = exercises.associateBy { it.id }
        val prs = records.mapNotNull { record ->
            val exercise = exerciseById[record.exerciseId] ?: return@mapNotNull null
            ExercisePR(
                exerciseId = record.exerciseId,
                exerciseName = exercise.name,
                muscleGroup = exercise.muscleGroup,
                maxWeightKg = record.maxWeightKg,
                maxWeightReps = record.maxWeightReps,
                maxWeightDate = record.maxWeightDate,
                maxVolumeKg = record.maxVolumeKg,
                maxVolumeReps = record.maxVolumeReps,
                maxVolumeWeightKg = record.maxVolumeWeightKg,
                maxVolumeDate = record.maxVolumeDate
            )
        }
            .sortedByDescending { it.maxWeightKg }
            .take(5)

        return HomeStats(workoutsThisWeek, streak, totalVolume, prs)
    }

    private fun isThisWeek(dateMillis: Long): Boolean {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = dateMillis }
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == then.get(Calendar.WEEK_OF_YEAR)
    }


}
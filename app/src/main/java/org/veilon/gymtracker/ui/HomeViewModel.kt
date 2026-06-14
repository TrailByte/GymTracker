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

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val templateDao = AppDatabase.getInstance(app).templateDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()

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
        UserPreferences.weeklyGoal(app)
    ) { sessions, logs, exercises, goal ->
        computeStats(sessions, logs, exercises, goal)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        HomeStats(0, 0, 0.0, emptyList())
    )

    private fun computeStats(
        sessions: List<WorkoutSession>,
        logs: List<ExerciseLog>,
        exercises: List<Exercise>,
        weeklyGoal: Int
    ): HomeStats {
        val workoutsThisWeek = sessions.count { isThisWeek(it.date) }
        val totalVolume = logs.sumOf { it.weight * it.reps }
        val streak = computeWeekStreak(sessions.map { it.date }, weeklyGoal)

        // PRs: per exercise, best single-set weight and best single-set volume
        val byExercise = logs.groupBy { it.exerciseId }
        val prs = byExercise.mapNotNull { (exId, exLogs) ->
            val exercise = exercises.find { it.id == exId } ?: return@mapNotNull null
            val maxWeight = exLogs.maxByOrNull { it.weight } ?: return@mapNotNull null
            val maxVol = exLogs.maxByOrNull { it.weight * it.reps } ?: return@mapNotNull null
            ExercisePR(
                exerciseName = exercise.name,
                muscleGroup = exercise.muscleGroup,
                maxWeightKg = maxWeight.weight,
                maxVolumeKg = maxVol.weight * maxVol.reps,
                maxVolumeReps = maxVol.reps,
                maxVolumeWeightKg = maxVol.weight
            )
        }
            // Most impressive first (by weight); cap to a handful for Home
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

    // Count consecutive weeks (ending this week) that met the goal
    private fun computeWeekStreak(dates: List<Long>, goal: Int): Int {
        if (dates.isEmpty()) return 0
        // Bucket workout counts by "weeks ago from now"
        val counts = HashMap<Int, Int>()
        val now = Calendar.getInstance()
        val nowWeekKey = now.get(Calendar.YEAR) * 100 + now.get(Calendar.WEEK_OF_YEAR)
        dates.forEach { d ->
            val c = Calendar.getInstance().apply { timeInMillis = d }
            val key = c.get(Calendar.YEAR) * 100 + c.get(Calendar.WEEK_OF_YEAR)
            val weeksAgo = weeksBetween(c, now)
            if (key <= nowWeekKey) counts[weeksAgo] = (counts[weeksAgo] ?: 0) + 1
        }
        var streak = 0
        var w = 0
        while ((counts[w] ?: 0) >= goal) { streak++; w++ }
        return streak
    }

    private fun weeksBetween(from: Calendar, to: Calendar): Int {
        val fromKey = from.get(Calendar.YEAR) * 53 + from.get(Calendar.WEEK_OF_YEAR)
        val toKey = to.get(Calendar.YEAR) * 53 + to.get(Calendar.WEEK_OF_YEAR)
        return toKey - fromKey
    }
}
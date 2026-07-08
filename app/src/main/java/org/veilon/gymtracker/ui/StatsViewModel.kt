package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.veilon.gymtracker.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeekPoint(val label: String, val value: Double)

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()
    private val recordDao = AppDatabase.getInstance(app).recordDao()

    val useLbs = UserPreferences.useLbs(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Every exercise's stored PR, joined with its name/muscle group.
    // Reads straight from the records table — O(records), never a log scan.
    val prs = combine(
        exerciseDao.getAll(),
        recordDao.getAllRecords()
    ) { exercises, records ->
        val exerciseById = exercises.associateBy { it.id }
        records.mapNotNull { record ->
            val exercise = exerciseById[record.exerciseId] ?: return@mapNotNull null
            ExercisePR(
                exerciseId = record.exerciseId,
                exerciseName = exercise.name,
                equipmentType = exercise.equipmentType,
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Last 8 weeks: total volume per week, and workouts per week.
    // NOTE: like Home's existing "This Week"/streak stats, this reads the full
    // sessions + completed-logs tables. That's the same known scale tradeoff
    // already tracked in TODO.md ("Scale / performance") — fine at today's data
    // size, worth revisiting with windowed queries once history gets large.
    val weeklyVolume = combine(
        workoutDao.getAllSessions(),
        workoutDao.getAllCompletedLogs()
    ) { sessions, logs ->
        buildWeeklyVolume(sessions.associate { it.id to it.date }, logs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyFrequency = workoutDao.getAllSessions()
        .map { sessions -> buildWeeklyFrequency(sessions.map { it.date }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun weekStartMillis(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return cal.timeInMillis
    }

    private fun lastNWeekStarts(n: Int): List<Long> {
        val oneWeek = 7L * 24 * 60 * 60 * 1000
        val thisWeek = weekStartMillis(System.currentTimeMillis())
        return (0 until n).map { thisWeek - it * oneWeek }.reversed()
    }

    private fun buildWeeklyVolume(
        dateBySession: Map<Long, Long>,
        logs: List<org.veilon.gymtracker.data.ExerciseLog>
    ): List<WeekPoint> {
        val weekStarts = lastNWeekStarts(8)
        val volumeByWeek = LinkedHashMap<Long, Double>().apply { weekStarts.forEach { put(it, 0.0) } }
        logs.forEach { log ->
            val sessionDate = dateBySession[log.sessionId] ?: return@forEach
            val ws = weekStartMillis(sessionDate)
            if (volumeByWeek.containsKey(ws)) {
                volumeByWeek[ws] = volumeByWeek.getValue(ws) + log.weight * log.reps
            }
        }
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        return weekStarts.map { ws -> WeekPoint(fmt.format(Date(ws)), volumeByWeek.getValue(ws)) }
    }

    private fun buildWeeklyFrequency(sessionDates: List<Long>): List<WeekPoint> {
        val weekStarts = lastNWeekStarts(8)
        val countByWeek = LinkedHashMap<Long, Int>().apply { weekStarts.forEach { put(it, 0) } }
        sessionDates.forEach { d ->
            val ws = weekStartMillis(d)
            if (countByWeek.containsKey(ws)) {
                countByWeek[ws] = countByWeek.getValue(ws) + 1
            }
        }
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        return weekStarts.map { ws -> WeekPoint(fmt.format(Date(ws)), countByWeek.getValue(ws).toDouble()) }
    }

    val muscleGroupVolume: StateFlow<List<WeekPoint>> = combine(
        workoutDao.getAllSessions(),
        workoutDao.getAllCompletedLogs(),
        exerciseDao.getAll()
    ) { sessions, logs, exercises ->
        buildMuscleGroupVolume(sessions.associate { it.id to it.date }, logs, exercises)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildMuscleGroupVolume(
        dateBySession: Map<Long, Long>,
        logs: List<org.veilon.gymtracker.data.ExerciseLog>,
        exercises: List<org.veilon.gymtracker.data.Exercise>
    ): List<WeekPoint> {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val muscleGroupById = exercises.associate { it.id to it.muscleGroup }
        val volumeByGroup = LinkedHashMap<String, Double>()
        logs.forEach { log ->
            val sessionDate = dateBySession[log.sessionId] ?: return@forEach
            if (sessionDate < cutoff) return@forEach
            val group = muscleGroupById[log.exerciseId] ?: return@forEach
            volumeByGroup[group] = (volumeByGroup[group] ?: 0.0) + log.weight * log.reps
        }
        return volumeByGroup.entries
            .sortedByDescending { it.value }
            .map { WeekPoint(it.key, it.value) }
    }

}

package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.ExerciseLog
import org.veilon.gymtracker.data.WorkoutSession
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ActiveWorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext
    private val workoutDao = AppDatabase.getInstance(app).workoutDao()
    private val templateDao = AppDatabase.getInstance(app).templateDao()

    val activeSessionId = UserPreferences.activeSession(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeSession = UserPreferences.activeSession(getApplication())
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(null)
            else kotlinx.coroutines.flow.flowOf(workoutDao.getSession(id))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun startEmpty(name: String, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val id = workoutDao.insertSession(WorkoutSession(name = name))
            UserPreferences.setActiveSession(appContext, id)
            onStarted(id)
        }
    }

    // Create a session, copy the template's exercises into it as empty sets
    fun startFromTemplate(templateId: Long, name: String, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val sessionId = workoutDao.insertSession(WorkoutSession(name = name))
            val templateExercises = templateDao.getExercisesForTemplateOnce(templateId)
            templateExercises.forEach { te ->
                // Create targetSets empty rows per exercise, reps/weight 0 to fill in
                for (setNum in 1..te.targetSets) {
                    workoutDao.insertLog(
                        ExerciseLog(
                            sessionId = sessionId,
                            exerciseId = te.exerciseId,
                            setNumber = setNum,
                            reps = te.targetReps,
                            weight = 0.0,
                            completed = false
                        )
                    )
                }
            }
            UserPreferences.setActiveSession(appContext, sessionId)
            onStarted(sessionId)
        }
    }

    fun repeatSession(pastSessionId: Long, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val past = workoutDao.getSession(pastSessionId)
            val name = past?.name ?: "Workout"
            val newId = workoutDao.insertSession(WorkoutSession(name = name))
            val pastLogs = workoutDao.getLogsForSessionOnce(pastSessionId)
            pastLogs.forEach { log ->
                workoutDao.insertLog(
                    ExerciseLog(
                        sessionId = newId,
                        exerciseId = log.exerciseId,
                        setNumber = log.setNumber,
                        reps = log.reps,
                        weight = log.weight,
                        completed = false
                    )
                )
            }
            UserPreferences.setActiveSession(appContext, newId)
            onStarted(newId)
        }
    }

    fun clearActive() {
        viewModelScope.launch {
            UserPreferences.setActiveSession(appContext, null)
        }
    }
}
package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.Exercise

class ExerciseLibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).exerciseDao()

    val muscleGroups = listOf("Chest", "Back", "Shoulders", "Legs", "Arms", "Core")

    val exercises = dao.getAllIncludingArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExercise(name: String, muscleGroup: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.insert(Exercise(name = name.trim(), muscleGroup = muscleGroup))
        }
    }

    fun updateExercise(exercise: Exercise, name: String, muscleGroup: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.update(exercise.copy(name = name.trim(), muscleGroup = muscleGroup))
        }
    }

    // Delete if no history; otherwise archive to preserve logged sets
    fun removeExercise(exercise: Exercise, onResult: (archived: Boolean) -> Unit) {
        viewModelScope.launch {
            val count = dao.logCount(exercise.id)
            if (count > 0) {
                dao.update(exercise.copy(archived = true))
                onResult(true)
            } else {
                dao.delete(exercise)
                onResult(false)
            }
        }
    }

    fun restoreExercise(exercise: Exercise) {
        viewModelScope.launch { dao.update(exercise.copy(archived = false)) }
    }
}
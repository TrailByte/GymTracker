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

    fun removeExercise(
        exercise: Exercise,
        onBlocked: () -> Unit,   // has history → can't delete, suggest archive
        onDeleted: () -> Unit
    ) {
        viewModelScope.launch {
            val count = dao.logCount(exercise.id)
            if (count > 0) {
                // Has logged history — protect it. Block delete, suggest archive.
                onBlocked()
            } else {
                // No history — safe to truly delete. Remove template links first
                // (so no foreign-key violation), then delete the exercise.
                dao.removeFromAllTemplates(exercise.id)
                dao.delete(exercise)
                onDeleted()
            }
        }
    }

    fun archiveExercise(exercise: Exercise) {
        viewModelScope.launch {
            dao.update(exercise.copy(archived = true))
        }
    }
    fun restoreExercise(exercise: Exercise) {
        viewModelScope.launch { dao.update(exercise.copy(archived = false)) }
    }
}
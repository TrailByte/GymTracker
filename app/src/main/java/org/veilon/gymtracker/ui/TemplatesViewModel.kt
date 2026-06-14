package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.TemplateExercise
import org.veilon.gymtracker.data.WorkoutTemplate

class TemplatesViewModel(app: Application) : AndroidViewModel(app) {

    private val templateDao = AppDatabase.getInstance(app).templateDao()
    private val exerciseDao = AppDatabase.getInstance(app).exerciseDao()

    val templates = templateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All active exercises, for the picker
    val allExercises = exerciseDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTemplate(name: String) {
        viewModelScope.launch {
            templateDao.insertTemplate(WorkoutTemplate(name = name))
        }
    }

    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            templateDao.deleteTemplate(template)
        }
    }

    fun templateExercises(templateId: Long) =
        templateDao.getExercisesForTemplate(templateId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExerciseToTemplate(templateId: Long, exerciseId: Long, currentCount: Int) {
        viewModelScope.launch {
            templateDao.insertTemplateExercise(
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = exerciseId,
                    orderIndex = currentCount
                )
            )
        }
    }

    fun removeExerciseFromTemplate(templateExercise: TemplateExercise) {
        viewModelScope.launch {
            templateDao.deleteTemplateExercise(templateExercise)
        }
    }

    // Persist a reordered list by rewriting orderIndex for each row
    fun saveReorder(reordered: List<TemplateExercise>) {
        viewModelScope.launch {
            val withNewOrder = reordered.mapIndexed { index, te ->
                te.copy(orderIndex = index)
            }
            templateDao.updateTemplateExercises(withNewOrder)
        }
    }
}
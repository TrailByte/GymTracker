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

    val templates = templateDao.getAllTemplates()
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

    fun addExerciseToTemplate(templateId: Long, exerciseId: Long, orderIndex: Int) {
        viewModelScope.launch {
            templateDao.insertTemplateExercise(
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = exerciseId,
                    orderIndex = orderIndex
                )
            )
        }
    }

    fun getExercisesForTemplate(templateId: Long) =
        templateDao.getExercisesForTemplate(templateId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
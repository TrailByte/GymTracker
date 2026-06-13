package org.veilon.gymtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM workout_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<WorkoutTemplate>>

    @Insert
    suspend fun insertTemplate(template: WorkoutTemplate): Long

    @Delete
    suspend fun deleteTemplate(template: WorkoutTemplate)

    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY orderIndex ASC")
    fun getExercisesForTemplate(templateId: Long): Flow<List<TemplateExercise>>

    @Insert
    suspend fun insertTemplateExercise(exercise: TemplateExercise): Long

    @Delete
    suspend fun deleteTemplateExercise(exercise: TemplateExercise)
}
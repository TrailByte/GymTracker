package org.veilon.gymtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    // Active (non-archived) — used by workout picker and template editor
    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY muscleGroup, name ASC")
    fun getAll(): Flow<List<Exercise>>

    // Everything including archived — for the Library screen
    @Query("SELECT * FROM exercises ORDER BY archived ASC, muscleGroup, name ASC")
    fun getAllIncludingArchived(): Flow<List<Exercise>>

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    // How many logs reference this exercise — decides delete vs archive
    @Query("SELECT COUNT(*) FROM exercise_logs WHERE exerciseId = :exerciseId")
    suspend fun logCount(exerciseId: Long): Int
}
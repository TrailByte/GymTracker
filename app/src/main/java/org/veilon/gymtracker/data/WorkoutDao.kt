package org.veilon.gymtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    @Insert
    suspend fun insertLog(log: ExerciseLog): Long

    @Query("SELECT * FROM exercise_logs WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    fun getLogsForSession(sessionId: Long): Flow<List<ExerciseLog>>

    @Query("""
        SELECT * FROM exercise_logs 
        WHERE exerciseId = :exerciseId 
        ORDER BY rowid DESC
    """)
    fun getLogsForExercise(exerciseId: Long): Flow<List<ExerciseLog>>

    @Delete
    suspend fun deleteLog(log: ExerciseLog)
}
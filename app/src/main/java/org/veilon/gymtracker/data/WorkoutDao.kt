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

    @Update
    suspend fun updateLog(log: ExerciseLog)

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Delete
    suspend fun deleteLog(log: ExerciseLog)

    @Query("SELECT * FROM exercise_logs WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    fun getLogsForSession(sessionId: Long): Flow<List<ExerciseLog>>

    @Query("""
        SELECT * FROM exercise_logs 
        WHERE exerciseId = :exerciseId 
        ORDER BY rowid DESC
    """)
    fun getLogsForExercise(exerciseId: Long): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs")
    fun getAllLogs(): kotlinx.coroutines.flow.Flow<List<ExerciseLog>>
    @Query("DELETE FROM exercise_logs WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteExerciseFromSession(sessionId: Long, exerciseId: Long)

    @Query("DELETE FROM exercise_logs WHERE sessionId = :sessionId AND completed = 0")
    suspend fun deleteIncompleteLogs(sessionId: Long)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSession?

    @Query("SELECT * FROM exercise_logs WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    suspend fun getLogsForSessionOnce(sessionId: Long): List<ExerciseLog>

    @Query("SELECT * FROM exercise_logs WHERE completed = 1")
    fun getAllCompletedLogs(): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs WHERE completed = 1")
    suspend fun getAllCompletedLogsOnce(): List<ExerciseLog>

    @Query("SELECT * FROM session_exercise_order WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getExerciseOrder(sessionId: Long): Flow<List<SessionExerciseOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseOrder(entries: List<SessionExerciseOrder>)
}
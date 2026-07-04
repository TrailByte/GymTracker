package org.veilon.gymtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM exercise_records WHERE exerciseId = :exerciseId")
    suspend fun getRecord(exerciseId: Long): ExerciseRecord?

    @Query("SELECT * FROM exercise_records")
    fun getAllRecords(): Flow<List<ExerciseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: ExerciseRecord)
}
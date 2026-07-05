package org.veilon.gymtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT achievementId FROM unlocked_achievements")
    fun getUnlockedIds(): Flow<List<String>>

    @Query("SELECT achievementId FROM unlocked_achievements")
    suspend fun getUnlockedIdsOnce(): List<String>

    @Query("SELECT * FROM unlocked_achievements")
    fun getAllUnlocked(): Flow<List<UnlockedAchievement>>

    // IGNORE, not REPLACE: an achievement's unlock date is permanent — if it
    // somehow gets "unlocked" again, keep the original date, don't overwrite it.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(entry: UnlockedAchievement)
}

package org.veilon.gymtracker.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity

/**
 * All-time personal record for one exercise: best single-set weight, and best
 * single-set volume (weight x reps), each with when it was achieved. Updated
 * incrementally whenever a set is completed — O(1) check-and-update, never a
 * scan across history. Home's "Recent PRs", the Stats screen, and (later) the
 * Log-screen PR pill all read from this same table.
 */
@Immutable
@Entity(tableName = "exercise_records", primaryKeys = ["exerciseId"])
data class ExerciseRecord(
    val exerciseId: Long,
    val maxWeightKg: Double,
    val maxWeightReps: Int,
    val maxWeightDate: Long,
    val maxVolumeKg: Double,
    val maxVolumeWeightKg: Double,
    val maxVolumeReps: Int,
    val maxVolumeDate: Long
)
package org.veilon.gymtracker.data

import androidx.room.Entity

/**
 * Explicit user-chosen exercise order within a workout session.
 * One row per (session, exercise). If no rows exist for a session (e.g. any
 * workout logged before this feature existed), the UI falls back to ordering
 * exercises by when they were first logged — so old workouts are unaffected.
 */
@Entity(tableName = "session_exercise_order", primaryKeys = ["sessionId", "exerciseId"])
data class SessionExerciseOrder(
    val sessionId: Long,
    val exerciseId: Long,
    val orderIndex: Int
)

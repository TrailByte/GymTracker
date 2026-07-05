package org.veilon.gymtracker.data

import androidx.room.Entity

/**
 * A permanently-unlocked achievement. Achievement DEFINITIONS (name, description,
 * criteria) live as plain Kotlin objects in gamification/Achievements.kt — they
 * don't vary per user, so there's nothing to store for them. This table only
 * records the one genuine per-user fact: which ones you've unlocked, and when.
 */
@Entity(tableName = "unlocked_achievements", primaryKeys = ["achievementId"])
data class UnlockedAchievement(
    val achievementId: String,
    val unlockedDate: Long
)

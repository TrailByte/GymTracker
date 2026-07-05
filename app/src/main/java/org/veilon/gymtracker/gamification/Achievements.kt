package org.veilon.gymtracker.gamification

/**
 * What kind of running total an achievement's threshold is checked against.
 * Adding a new achievement is just adding a row to Achievements.ALL below —
 * no other code needs to change.
 */
enum class AchievementType {
    WORKOUT_COUNT,
    STREAK_WEEKS,
    PR_COUNT,
    LIFETIME_VOLUME_KG
}

data class AchievementDef(
    val id: String,           // stable forever — this is the DB key, never rename
    val name: String,
    val description: String,
    val type: AchievementType,
    val threshold: Long
)

object Achievements {
    val ALL: List<AchievementDef> = listOf(
        AchievementDef("workouts_1", "First Workout", "Complete your first workout", AchievementType.WORKOUT_COUNT, 1),
        AchievementDef("workouts_10", "10 Workouts", "Complete 10 workouts", AchievementType.WORKOUT_COUNT, 10),
        AchievementDef("workouts_50", "50 Workouts", "Complete 50 workouts", AchievementType.WORKOUT_COUNT, 50),
        AchievementDef("workouts_100", "100 Workouts", "Complete 100 workouts", AchievementType.WORKOUT_COUNT, 100),

        AchievementDef("streak_2", "2-Week Streak", "Keep a 2-week streak", AchievementType.STREAK_WEEKS, 2),
        AchievementDef("streak_4", "4-Week Streak", "Keep a 4-week streak", AchievementType.STREAK_WEEKS, 4),
        AchievementDef("streak_8", "8-Week Streak", "Keep an 8-week streak", AchievementType.STREAK_WEEKS, 8),

        AchievementDef("pr_1", "First PR", "Set your first personal record", AchievementType.PR_COUNT, 1),
        AchievementDef("pr_10", "10 PRs", "Set 10 personal records", AchievementType.PR_COUNT, 10),
        AchievementDef("pr_50", "50 PRs", "Set 50 personal records", AchievementType.PR_COUNT, 50),

        AchievementDef("volume_10k", "10,000kg Lifted", "Lift a lifetime total of 10,000kg", AchievementType.LIFETIME_VOLUME_KG, 10_000),
        AchievementDef("volume_50k", "50,000kg Lifted", "Lift a lifetime total of 50,000kg", AchievementType.LIFETIME_VOLUME_KG, 50_000),
        AchievementDef("volume_100k", "100,000kg Lifted", "Lift a lifetime total of 100,000kg", AchievementType.LIFETIME_VOLUME_KG, 100_000)
    )
}

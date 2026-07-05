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
        // Workouts
        AchievementDef("workouts_1", "First Workout", "Complete your first workout", AchievementType.WORKOUT_COUNT, 1),
        AchievementDef("workouts_5", "5 Workouts", "Complete 5 workouts", AchievementType.WORKOUT_COUNT, 5),
        AchievementDef("workouts_10", "10 Workouts", "Complete 10 workouts", AchievementType.WORKOUT_COUNT, 10),
        AchievementDef("workouts_25", "25 Workouts", "Complete 25 workouts", AchievementType.WORKOUT_COUNT, 25),
        AchievementDef("workouts_50", "50 Workouts", "Complete 50 workouts", AchievementType.WORKOUT_COUNT, 50),
        AchievementDef("workouts_100", "100 Workouts", "Complete 100 workouts", AchievementType.WORKOUT_COUNT, 100),
        AchievementDef("workouts_250", "250 Workouts", "Complete 250 workouts", AchievementType.WORKOUT_COUNT, 250),
        AchievementDef("workouts_500", "500 Workouts", "Complete 500 workouts", AchievementType.WORKOUT_COUNT, 500),
        AchievementDef("workouts_1000", "1000 Workouts", "Complete 1000 workouts", AchievementType.WORKOUT_COUNT, 1000),

        // Streaks
        AchievementDef("streak_1", "First Week", "Keep a 1-week streak", AchievementType.STREAK_WEEKS, 1),
        AchievementDef("streak_2", "2-Week Streak", "Keep a 2-week streak", AchievementType.STREAK_WEEKS, 2),
        AchievementDef("streak_3", "3-Week Streak", "Keep a 3-week streak", AchievementType.STREAK_WEEKS, 3),
        AchievementDef("streak_4", "4-Week Streak", "Keep a 4-week streak", AchievementType.STREAK_WEEKS, 4),
        AchievementDef("streak_6", "6-Week Streak", "Keep a 6-week streak", AchievementType.STREAK_WEEKS, 6),
        AchievementDef("streak_8", "8-Week Streak", "Keep an 8-week streak", AchievementType.STREAK_WEEKS, 8),
        AchievementDef("streak_12", "12-Week Streak", "Keep a 12-week streak", AchievementType.STREAK_WEEKS, 12),
        AchievementDef("streak_26", "26-Week Streak", "Keep a 26-week streak", AchievementType.STREAK_WEEKS, 26),
        AchievementDef("streak_52", "1-Year Streak", "Keep a 52-week streak", AchievementType.STREAK_WEEKS, 52),

        // PRs
        AchievementDef("pr_1", "First PR", "Set your first personal record", AchievementType.PR_COUNT, 1),
        AchievementDef("pr_5", "5 PRs", "Set 5 personal records", AchievementType.PR_COUNT, 5),
        AchievementDef("pr_10", "10 PRs", "Set 10 personal records", AchievementType.PR_COUNT, 10),
        AchievementDef("pr_25", "25 PRs", "Set 25 personal records", AchievementType.PR_COUNT, 25),
        AchievementDef("pr_50", "50 PRs", "Set 50 personal records", AchievementType.PR_COUNT, 50),
        AchievementDef("pr_100", "100 PRs", "Set 100 personal records", AchievementType.PR_COUNT, 100),
        AchievementDef("pr_250", "250 PRs", "Set 250 personal records", AchievementType.PR_COUNT, 250),

        // Lifetime volume
        AchievementDef("volume_1k", "1,000kg Lifted", "Lift a lifetime total of 1,000kg", AchievementType.LIFETIME_VOLUME_KG, 1_000),
        AchievementDef("volume_10k", "10,000kg Lifted", "Lift a lifetime total of 10,000kg", AchievementType.LIFETIME_VOLUME_KG, 10_000),
        AchievementDef("volume_25k", "25,000kg Lifted", "Lift a lifetime total of 25,000kg", AchievementType.LIFETIME_VOLUME_KG, 25_000),
        AchievementDef("volume_50k", "50,000kg Lifted", "Lift a lifetime total of 50,000kg", AchievementType.LIFETIME_VOLUME_KG, 50_000),
        AchievementDef("volume_100k", "100,000kg Lifted", "Lift a lifetime total of 100,000kg", AchievementType.LIFETIME_VOLUME_KG, 100_000),
        AchievementDef("volume_250k", "250,000kg Lifted", "Lift a lifetime total of 250,000kg", AchievementType.LIFETIME_VOLUME_KG, 250_000),
        AchievementDef("volume_500k", "500,000kg Lifted", "Lift a lifetime total of 500,000kg", AchievementType.LIFETIME_VOLUME_KG, 500_000),
        AchievementDef("volume_1m", "1,000,000kg Lifted", "Lift a lifetime total of one million kg", AchievementType.LIFETIME_VOLUME_KG, 1_000_000)
    )
}

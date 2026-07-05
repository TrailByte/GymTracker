package org.veilon.gymtracker.gamification

import android.content.Context
import kotlinx.coroutines.flow.first
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.UnlockedAchievement
import org.veilon.gymtracker.ui.UserPreferences
import org.veilon.gymtracker.ui.computeWeekStreak

/**
 * Central place for gamification rules. Kept as a plain object (not a
 * ViewModel) so any ViewModel can call into it without duplicating logic —
 * the alternative was rules scattered across WorkoutViewModel, HomeViewModel,
 * etc., which is exactly how the week-streak bug happened before.
 *
 * Level is DERIVED from total XP, never stored — see levelForXp() below.
 * Achievement unlocks ARE stored (unlocked_achievements table), because
 * "which ones, and when" is a genuine fact that can't be recomputed later.
 */
object GamificationEngine {

    // --- XP amounts (tune freely — nothing else depends on these numbers) ---
    const val XP_PER_WORKOUT = 20L
    const val XP_PER_PR = 15L
    const val XP_PER_STREAK_WEEK = 30L

    // --- Level curve: reaching level L costs 50*L*(L-1) total XP, i.e. the
    // step from level N to N+1 costs N*100. Capped at 30 per "season" —
    // beyond that, XP still accumulates but level display holds at 30 until
    // the user chooses to prestige. ---
    const val MAX_SEASON_LEVEL = 30

    fun xpNeededForLevel(level: Int): Long = 50L * level * (level - 1)

    fun levelForXp(xp: Long): Int {
        var level = 1
        while (level < MAX_SEASON_LEVEL && xp >= xpNeededForLevel(level + 1)) level++
        return level
    }

    /** XP progress within the current level, for a progress bar. Null once maxed. */
    fun progressWithinLevel(xp: Long): Triple<Long, Long, Int>? {
        val level = levelForXp(xp)
        if (level >= MAX_SEASON_LEVEL) return null
        val floor = xpNeededForLevel(level)
        val ceiling = xpNeededForLevel(level + 1)
        return Triple(xp - floor, ceiling - floor, level)
    }

    // --- Event hooks ---

    /** Call when a workout finishes. Awards workout XP, checks workout-count,
     *  lifetime-volume, and streak-increase achievements. */
    suspend fun onWorkoutFinished(context: Context) {
        addXp(context, XP_PER_WORKOUT)

        val db = AppDatabase.getInstance(context)
        val sessions = db.workoutDao().getAllSessions().first()
        checkThresholdAchievements(context, AchievementType.WORKOUT_COUNT, sessions.size.toLong())

        val logs = db.workoutDao().getAllCompletedLogsOnce()
        val totalVolume = logs.sumOf { it.weight * it.reps }
        checkThresholdAchievements(context, AchievementType.LIFETIME_VOLUME_KG, totalVolume.toLong())

        val weeklyGoal = UserPreferences.weeklyGoal(context).first()
        val newStreak = computeWeekStreak(sessions.map { it.date }, weeklyGoal)
        val lastStreak = UserPreferences.lastKnownStreak(context).first()
        if (newStreak > lastStreak) {
            addXp(context, XP_PER_STREAK_WEEK * (newStreak - lastStreak))
            checkThresholdAchievements(context, AchievementType.STREAK_WEEKS, newStreak.toLong())
        }
        UserPreferences.setLastKnownStreak(context, newStreak)
    }

    /** Call once for each record (weight/volume) a completed set actually improves.
     *  count=0: nothing to do. count=1: one of the two improved. count=2: both did. */
    suspend fun onPrBroken(context: Context, count: Int) {
        if (count <= 0) return
        repeat(count) { addXp(context, XP_PER_PR) }
        val newTotal = UserPreferences.totalPrCount(context).first() + count
        UserPreferences.setTotalPrCount(context, newTotal)
        checkThresholdAchievements(context, AchievementType.PR_COUNT, newTotal)
    }

    // --- One-time historical backfill (mirrors the exercise_records backfill) ---

    /** Runs once ever: replays existing history to (a) count real historical PR
     *  events, and (b) retroactively unlock any achievement already earned
     *  before this feature existed (e.g. someone with 40 logged workouts
     *  should immediately have "10 Workouts" unlocked, not wait for #41). */
    // Bump this any time backfill logic changes materially — it forces every
    // installed phone to re-run the (corrected) calculation once, automatically.
    private const val CURRENT_BACKFILL_VERSION = 3

    suspend fun backfillIfNeeded(context: Context) {
        val doneVersion = UserPreferences.gamificationBackfillVersion(context).first()
        if (doneVersion >= CURRENT_BACKFILL_VERSION) return

        val db = AppDatabase.getInstance(context)
        val sessions = db.workoutDao().getAllSessions().first()
        val dateBySession = sessions.associate { it.id to it.date }
        val allLogs = db.workoutDao().getAllCompletedLogsOnce()

        // Replay each exercise's history chronologically, counting every time
        // a new best (weight or volume) was set — that's a historical PR event.
        var totalPrEvents = 0L
        allLogs.groupBy { it.exerciseId }.forEach { (_, exLogs) ->
            val sorted = exLogs.sortedBy { dateBySession[it.sessionId] ?: 0L }
            var bestWeight = 0.0
            var bestVolume = 0.0
            sorted.forEach { log ->
                if (log.weight > bestWeight) { bestWeight = log.weight; totalPrEvents++ }
                val vol = log.weight * log.reps
                if (vol > bestVolume) { bestVolume = vol; totalPrEvents++ }
            }
        }
        UserPreferences.setTotalPrCount(context, totalPrEvents)
        checkThresholdAchievements(context, AchievementType.PR_COUNT, totalPrEvents)

        checkThresholdAchievements(context, AchievementType.WORKOUT_COUNT, sessions.size.toLong())
        val totalVolume = allLogs.sumOf { it.weight * it.reps }
        checkThresholdAchievements(context, AchievementType.LIFETIME_VOLUME_KG, totalVolume.toLong())

        val weeklyGoal = UserPreferences.weeklyGoal(context).first()
        val currentStreak = computeWeekStreak(sessions.map { it.date }, weeklyGoal)
        checkThresholdAchievements(context, AchievementType.STREAK_WEEKS, currentStreak.toLong())
        UserPreferences.setLastKnownStreak(context, currentStreak)

        // THE FIX: actually award the XP this history represents. This is an
        // absolute recompute-and-SET (not an incremental add), which is what
        // makes it safe to bump CURRENT_BACKFILL_VERSION again in the future
        // without worrying about double-counting whatever's already stored.
        val recomputedXp = sessions.size * XP_PER_WORKOUT +
                totalPrEvents * XP_PER_PR +
                currentStreak * XP_PER_STREAK_WEEK
        UserPreferences.setTotalXp(context, recomputedXp)

        UserPreferences.setGamificationBackfillVersion(context, CURRENT_BACKFILL_VERSION)
    }

    // --- Internals ---

    private suspend fun addXp(context: Context, amount: Long) {
        val current = UserPreferences.totalXp(context).first()
        UserPreferences.setTotalXp(context, current + amount)
    }

    private suspend fun checkThresholdAchievements(context: Context, type: AchievementType, currentValue: Long) {
        val db = AppDatabase.getInstance(context)
        val alreadyUnlocked = db.achievementDao().getUnlockedIdsOnce().toSet()
        Achievements.ALL
            .filter { it.type == type && currentValue >= it.threshold && it.id !in alreadyUnlocked }
            .forEach { achievement ->
                db.achievementDao().unlock(UnlockedAchievement(achievement.id, System.currentTimeMillis()))
            }
    }
}

package org.veilon.gymtracker.gamification

import android.content.Context
import kotlinx.coroutines.flow.first
import org.veilon.gymtracker.data.AppDatabase
import org.veilon.gymtracker.data.UnlockedAchievement
import org.veilon.gymtracker.ui.UserPreferences
import org.veilon.gymtracker.ui.computeWeekStreak
import org.veilon.gymtracker.ui.theme.ThemeUnlocks

/**
 * Central place for gamification rules. Kept as a plain object (not a
 * ViewModel) so any ViewModel can call into it without duplicating logic —
 * the alternative was rules scattered across WorkoutViewModel, HomeViewModel,
 * etc., which is exactly how the week-streak bug happened before.
 *
 * Level is DERIVED from total XP, never stored — see levelForXp() below.
 * Achievement unlocks ARE stored (unlocked_achievements table), because
 * "which ones, and when" is a genuine fact that can't be recomputed later.
 *
 * Live event hooks (onWorkoutFinished, onPrBroken) push whatever happened to
 * CelebrationBus so the UI can show a full-screen moment. The historical
 * backfill deliberately does NOT push celebrations — retroactive credit on
 * first launch after an update shouldn't trigger a barrage of popups.
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

    // --- Event hooks (live — these DO push celebrations) ---

    /** Call when a workout finishes. Awards workout XP, checks workout-count,
     *  lifetime-volume, and streak-increase achievements, and shows a
     *  celebration for anything newly earned. */
    suspend fun onWorkoutFinished(context: Context, sessionName: String, durationSeconds: Long?) {
        // Always fires, unlike level-up/achievements which are conditional —
        // finishing a workout deserves acknowledgment every time.
        val celebrations = mutableListOf<Celebration>(
            Celebration.WorkoutFinished(sessionName, durationSeconds)
        )
        var xpToAdd = XP_PER_WORKOUT

        val db = AppDatabase.getInstance(context)
        val sessions = db.workoutDao().getAllSessions().first()
        celebrations += checkThresholdAchievements(context, AchievementType.WORKOUT_COUNT, sessions.size.toLong())

        val logs = db.workoutDao().getAllCompletedLogsOnce()
        val totalVolume = logs.sumOf { it.weight * it.reps }
        celebrations += checkThresholdAchievements(context, AchievementType.LIFETIME_VOLUME_KG, totalVolume.toLong())

        val weeklyGoal = UserPreferences.weeklyGoal(context).first()
        val newStreak = computeWeekStreak(sessions.map { it.date }, weeklyGoal)
        val lastStreak = UserPreferences.lastKnownStreak(context).first()
        if (newStreak > lastStreak) {
            xpToAdd += XP_PER_STREAK_WEEK * (newStreak - lastStreak)
            celebrations += checkThresholdAchievements(context, AchievementType.STREAK_WEEKS, newStreak.toLong())
        }
        UserPreferences.setLastKnownStreak(context, newStreak)

        addXp(context, xpToAdd)?.let { celebrations += it }

        CelebrationBus.push(celebrations)
    }

    /** Call once for each record (weight/volume) a completed set actually improves.
     *  count=0: nothing to do. count=1: one of the two improved. count=2: both did. */
    suspend fun onPrBroken(context: Context, count: Int) {
        if (count <= 0) return
        val celebrations = mutableListOf<Celebration>()

        val newTotal = UserPreferences.totalPrCount(context).first() + count
        UserPreferences.setTotalPrCount(context, newTotal)
        celebrations += checkThresholdAchievements(context, AchievementType.PR_COUNT, newTotal)

        addXp(context, XP_PER_PR * count)?.let { celebrations += it }

        CelebrationBus.push(celebrations)
    }

    // --- One-time historical backfill (mirrors the exercise_records backfill).
    // Silent by design — see class doc above. ---

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
        checkThresholdAchievements(context, AchievementType.PR_COUNT, totalPrEvents) // return value ignored: silent

        checkThresholdAchievements(context, AchievementType.WORKOUT_COUNT, sessions.size.toLong())
        val totalVolume = allLogs.sumOf { it.weight * it.reps }
        checkThresholdAchievements(context, AchievementType.LIFETIME_VOLUME_KG, totalVolume.toLong())

        val weeklyGoal = UserPreferences.weeklyGoal(context).first()
        val currentStreak = computeWeekStreak(sessions.map { it.date }, weeklyGoal)
        checkThresholdAchievements(context, AchievementType.STREAK_WEEKS, currentStreak.toLong())
        UserPreferences.setLastKnownStreak(context, currentStreak)

        // Absolute recompute-and-SET (not incremental add) — safe to re-run
        // any number of times without double-counting.
        val recomputedXp = sessions.size * XP_PER_WORKOUT +
                totalPrEvents * XP_PER_PR +
                currentStreak * XP_PER_STREAK_WEEK
        UserPreferences.setTotalXp(context, recomputedXp)

        UserPreferences.setGamificationBackfillVersion(context, CURRENT_BACKFILL_VERSION)
    }

    // --- Internals ---

    /** Adds XP; returns a LevelUp celebration if this crossed a level boundary
     *  (enriched with the theme name if this level also unlocks one), else null. */
    private suspend fun addXp(context: Context, amount: Long): Celebration.LevelUp? {
        val current = UserPreferences.totalXp(context).first()
        val oldLevel = levelForXp(current)
        val newTotal = current + amount
        UserPreferences.setTotalXp(context, newTotal)
        val newLevel = levelForXp(newTotal)

        if (newLevel <= oldLevel) return null
        val prestige = UserPreferences.prestigeLevel(context).first()
        val unlockedTheme = ThemeUnlocks.themeUnlockedAtLevel(newLevel)
        return Celebration.LevelUp(newLevel = newLevel, prestige = prestige, newTheme = unlockedTheme)
    }

    /** Unlocks any achievement of [type] whose threshold [currentValue] now
     *  meets, and returns celebrations for whichever ones were newly unlocked
     *  (empty list if none, or if all were already unlocked). */
    private suspend fun checkThresholdAchievements(
        context: Context,
        type: AchievementType,
        currentValue: Long
    ): List<Celebration.AchievementUnlocked> {
        val db = AppDatabase.getInstance(context)
        val alreadyUnlocked = db.achievementDao().getUnlockedIdsOnce().toSet()
        val newlyUnlocked = Achievements.ALL
            .filter { it.type == type && currentValue >= it.threshold && it.id !in alreadyUnlocked }
        newlyUnlocked.forEach { achievement ->
            db.achievementDao().unlock(UnlockedAchievement(achievement.id, System.currentTimeMillis()))
        }
        return newlyUnlocked.map { Celebration.AchievementUnlocked(it) }
    }
}

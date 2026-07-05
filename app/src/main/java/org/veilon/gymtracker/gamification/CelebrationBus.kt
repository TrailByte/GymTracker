package org.veilon.gymtracker.gamification

import kotlinx.coroutines.flow.MutableStateFlow
import org.veilon.gymtracker.ui.theme.ThemeTier

/**
 * A single celebratable moment. Deliberately NOT persisted anywhere — this is
 * an ephemeral "hey UI, show this now" signal for the current app session
 * only. Losing a pending celebration on app kill is fine and expected, same
 * as a snackbar would be.
 */
sealed class Celebration {
    data class LevelUp(
        val newLevel: Int,
        val prestige: Int,
        val newTheme: ThemeTier?   // non-null if this level also unlocked a theme
    ) : Celebration()

    data class AchievementUnlocked(val achievement: AchievementDef) : Celebration()
}

/**
 * Plain in-memory singleton, not a ViewModel — any ViewModel (WorkoutViewModel
 * today; possibly others later) can push into it without needing a shared
 * parent ViewModel or DataStore round-trip. MainActivity observes `pending`
 * and renders whichever celebration is first in the queue as a full-screen
 * overlay; multiple celebrations from the same event (e.g. level up AND an
 * achievement in the same workout) queue and show one at a time.
 */
object CelebrationBus {
    val pending = MutableStateFlow<List<Celebration>>(emptyList())

    fun push(celebrations: List<Celebration>) {
        if (celebrations.isEmpty()) return
        pending.value = pending.value + celebrations
    }

    fun dismissCurrent() {
        pending.value = pending.value.drop(1)
    }
}

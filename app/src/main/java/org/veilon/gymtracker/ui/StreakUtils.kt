package org.veilon.gymtracker.ui

import java.util.Calendar

// Normalize a timestamp to the start-of-week (midnight on the week's first day,
// locale-aware). Shared by anything that needs to bucket dates by week.
fun weekStartMillis(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    return cal.timeInMillis
}

// Count consecutive weeks (ending this week) that met the goal.
// Single source of truth — HomeViewModel (for display) and GamificationEngine
// (for streak-increase detection / achievements) both call this, so a fix here
// fixes it everywhere at once.
fun computeWeekStreak(dates: List<Long>, goal: Int): Int {
    if (dates.isEmpty() || goal <= 0) return 0

    val countsByWeekStart = HashMap<Long, Int>()
    dates.forEach { d ->
        val ws = weekStartMillis(d)
        countsByWeekStart[ws] = (countsByWeekStart[ws] ?: 0) + 1
    }

    val oneWeekMillis = 7L * 24 * 60 * 60 * 1000
    var cursorWeekStart = weekStartMillis(System.currentTimeMillis())
    var streak = 0
    while ((countsByWeekStart[cursorWeekStart] ?: 0) >= goal) {
        streak++
        cursorWeekStart -= oneWeekMillis
    }
    return streak
}

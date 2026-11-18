package com.navod718.healthmate.core.insights

import com.navod718.healthmate.feature.health.data.DailyBreakdown
import com.navod718.healthmate.feature.health.data.DailyGoals
import java.time.LocalDate

data class Achievement(val id: String, val title: String, val description: String, val unlocked: Boolean)

object Insights {
    // A complete day meets every goal, matching the dashboard "Goal reached" label.
    fun isComplete(day: DailyBreakdown, goals: DailyGoals) =
        day.steps >= goals.steps && day.calories >= goals.calories && day.water >= goals.water

    private fun completeDates(days: List<DailyBreakdown>, goals: DailyGoals): List<LocalDate> =
        days.filter { isComplete(it, goals) }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }

    // Counts consecutive complete days ending today. An unfinished today does not break the streak.
    fun currentStreak(days: List<DailyBreakdown>, goals: DailyGoals, today: LocalDate): Int {
        val complete = completeDates(days, goals).toSet()
        var cursor = if (today in complete) today else today.minusDays(1)
        var streak = 0
        while (cursor in complete) { streak++; cursor = cursor.minusDays(1) }
        return streak
    }

    fun bestStreak(days: List<DailyBreakdown>, goals: DailyGoals): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in completeDates(days, goals).sorted()) {
            run = if (previous != null && date == previous.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = date
        }
        return best
    }

    fun achievements(days: List<DailyBreakdown>, goals: DailyGoals, totalCount: Int): List<Achievement> {
        val best = bestStreak(days, goals)
        fun unlocked(id: String, title: String, description: String, condition: Boolean) =
            Achievement(id, title, description, condition)
        return listOf(
            unlocked("first-record", "First step", "Save your first record.", totalCount >= 1),
            unlocked("ten-records", "Getting going", "Save 10 records.", totalCount >= 10),
            unlocked("thirty-records", "Committed", "Save 30 records.", totalCount >= 30),
            unlocked("perfect-day", "Perfect day", "Meet all three goals in a single day.", days.any { isComplete(it, goals) }),
            unlocked("streak-3", "Hat-trick", "Meet every goal three days in a row.", best >= 3),
            unlocked("streak-7", "Week warrior", "Meet every goal seven days in a row.", best >= 7),
            unlocked("streak-14", "Fortnight force", "Meet every goal fourteen days in a row.", best >= 14),
            unlocked("steps-10k", "Ten thousand", "Log 10,000 steps in a single day.", days.any { it.steps >= 10_000 }),
        )
    }
}

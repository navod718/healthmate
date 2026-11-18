package com.navod718.healthmate

import com.navod718.healthmate.core.insights.Insights
import com.navod718.healthmate.feature.health.data.DailyBreakdown
import com.navod718.healthmate.feature.health.data.DailyGoals
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class InsightsTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val goals = DailyGoals()
    private fun day(date: LocalDate, steps: Long = 0, calories: Long = 0, water: Long = 0) =
        DailyBreakdown(date.toString(), steps, calories, water)
    private fun complete(date: LocalDate) = day(date, goals.steps.toLong(), goals.calories.toLong(), goals.water.toLong())

    @Test fun aDayCountsOnlyWhenEveryGoalIsMet() {
        assertTrue(Insights.isComplete(complete(today), goals))
        assertFalse(Insights.isComplete(day(today, water = goals.water.toLong()), goals))
    }
    @Test fun streakCountsConsecutiveCompleteDaysEndingToday() {
        val days = listOf(complete(today), complete(today.minusDays(1)), complete(today.minusDays(2)))
        assertEquals(3, Insights.currentStreak(days, goals, today))
    }
    @Test fun anIncompleteTodayDoesNotBreakTheStreak() {
        val days = listOf(day(today), complete(today.minusDays(1)), complete(today.minusDays(2)))
        assertEquals(2, Insights.currentStreak(days, goals, today))
    }
    @Test fun aGapEndsTheCurrentStreak() {
        val days = listOf(complete(today), complete(today.minusDays(2)))
        assertEquals(1, Insights.currentStreak(days, goals, today))
    }
    @Test fun bestStreakKeepsTheLongestRunAcrossGaps() {
        val days = listOf(
            complete(today.minusDays(10)), complete(today.minusDays(9)),
            complete(today.minusDays(2)),
        )
        assertEquals(2, Insights.bestStreak(days, goals))
    }
    @Test fun achievementsUnlockAtTheirThresholds() {
        assertTrue(Insights.achievements(emptyList(), goals, 0).none { it.unlocked })
        assertTrue(Insights.achievements(listOf(day(today, 100)), goals, 1).first { it.id == "first-record" }.unlocked)
        assertTrue(Insights.achievements(emptyList(), goals, 10).first { it.id == "ten-records" }.unlocked)
        assertTrue(Insights.achievements(emptyList(), goals, 9).first { it.id == "ten-records" }.unlocked.not())
        assertTrue(Insights.achievements(listOf(day(today, steps = 10_000)), goals, 1).first { it.id == "steps-10k" }.unlocked)
        val perfect = Insights.achievements(listOf(complete(today)), goals, 1)
        assertTrue(perfect.first { it.id == "perfect-day" }.unlocked)
        assertTrue(perfect.first { it.id == "streak-3" }.unlocked.not())
        val three = Insights.achievements(
            listOf(complete(today), complete(today.minusDays(1)), complete(today.minusDays(2))), goals, 3,
        )
        assertTrue(three.first { it.id == "streak-3" }.unlocked)
        assertTrue(three.first { it.id == "streak-7" }.unlocked.not())
    }
}

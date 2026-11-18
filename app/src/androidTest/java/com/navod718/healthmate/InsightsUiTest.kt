package com.navod718.healthmate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.navod718.healthmate.core.database.HealthDatabase
import com.navod718.healthmate.feature.health.data.*
import com.navod718.healthmate.feature.health.presentation.*
import com.navod718.healthmate.ui.theme.HealthMateTheme
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InsightsUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: HealthDatabase
    private lateinit var model: HealthViewModel
    private val store = ViewModelStore()
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthDatabase::class.java).build()
        model = HealthViewModel(HealthRepository(db.records()))
        store.put("insights", model)
    }
    @After fun close() { store.clear(); db.close() }
    private fun insert(date: String, steps: Int, calories: Int, water: Int): Long = runBlocking {
        db.records().insert(HealthRecord(date = date, steps = steps, calories = calories, water = water))
    }
    private fun launchInsights() {
        compose.setContent { HealthMateTheme { HealthMateApp(model) } }
        compose.onNodeWithText("Insights").performClick()
        compose.waitUntil(10_000) { !model.insights.value.loading }
    }
    @Test fun insightsShowsTrendStreakAndAchievementsForRecords() {
        val goals = DailyGoals()
        insert(LocalDate.now().toString(), goals.steps, goals.calories, goals.water)
        insert(LocalDate.now().minusDays(1).toString(), 1000, 50, 500)
        launchInsights()
        compose.onNodeWithTag("trend-chart").assertExists()
        compose.onNodeWithTag("streak-card").assertExists()
        compose.onNodeWithText("1-day streak").assertExists()
        compose.onNodeWithText("2 of 8 unlocked").assertExists()
        compose.onNodeWithTag("achievement-first-record").assertExists()
        compose.onNodeWithTag("achievement-ten-records").assertExists()
    }
    @Test fun insightsMetricAndRangeChipsStayRendered() {
        insert(LocalDate.now().toString(), 3000, 100, 600)
        launchInsights()
        compose.onNodeWithText("Calories").performClick()
        compose.onNodeWithText("30 days").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("trend-chart").assertExists()
        compose.onNodeWithText("Water").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("trend-chart").assertExists()
    }
    @Test fun insightsShowsEmptyStateWithoutRecords() {
        launchInsights()
        compose.onNodeWithText("No data yet").assertExists()
        compose.onNodeWithText("Add records or load the samples from the Records tab to see your trends, streak and achievements.").assertExists()
    }
}

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
import java.text.NumberFormat
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrendsAndWaterUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: HealthDatabase
    private lateinit var model: HealthViewModel
    private val store = ViewModelStore()
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthDatabase::class.java).build()
        model = HealthViewModel(HealthRepository(db.records())); store.put("trends",model)
    }
    @After fun close() { store.clear(); db.close() }
    private fun launch() {
        compose.setContent { HealthMateTheme { HealthMateApp(model) } }
        compose.waitUntil(10_000) { !model.totals.value.loading && !model.week.value.loading }
    }
    @Test fun quickWaterButtonsPersistSeparateRecordsAndRefreshDashboardAndWeek() {
        launch()
        compose.onNodeWithTag("water-250").performScrollTo().performClick()
        compose.waitUntil(10_000) { !model.busy.value && model.totals.value.totals.water == 250L }
        compose.onNodeWithTag("water-500").performScrollTo().performClick()
        compose.waitUntil(10_000) { !model.busy.value && model.totals.value.totals.water == 750L && model.week.value.days.lastOrNull()?.water == 750L }
        compose.onNodeWithText("750 ml").performScrollTo().assertIsDisplayed()
        val entries = runBlocking { db.records().count() }
        assertEquals(2, entries)
        compose.onNodeWithText("Records").performClick()
        compose.waitUntil(10_000) { model.records.value.records.size == 2 }
        compose.onNodeWithTag("records-list").performScrollToNode(hasText("500 ml water"))
        compose.onNodeWithText("500 ml water").assertIsDisplayed()
    }
    @Test fun trendMetricAndDaySelectionShowExactTotals() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        runBlocking {
            db.records().insert(HealthRecord(date=today.toString(),steps=1000,calories=50,water=500))
            db.records().insert(HealthRecord(date=yesterday.toString(),steps=2000,calories=100,water=1000))
        }
        launch()
        compose.onNodeWithTag("trend-metric-WATER").performScrollTo().performClick()
        compose.onNodeWithTag("trend-selected-value").performScrollTo().assertTextEquals("${displayDate(today.toString())}: 500 ml")
        compose.onNodeWithTag("trend-day-$yesterday").performScrollTo().performClick()
        compose.onNodeWithTag("trend-day-$yesterday").assertIsSelected()
        compose.onNodeWithTag("trend-selected-value").assertTextEquals("${displayDate(yesterday.toString())}: ${NumberFormat.getIntegerInstance().format(1000)} ml")
        compose.onNodeWithTag("trend-metric-CALORIES").performScrollTo().performClick()
        compose.onNodeWithTag("trend-selected-value").assertTextEquals("${displayDate(yesterday.toString())}: 100 kcal")
        compose.onNodeWithTag("trend-metric-STEPS").performScrollTo().performClick()
        compose.onNodeWithTag("trend-selected-value").assertTextEquals("${displayDate(yesterday.toString())}: ${NumberFormat.getIntegerInstance().format(2000)} steps")
    }
}

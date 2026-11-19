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
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: HealthDatabase
    private lateinit var model: HealthViewModel
    private val store = ViewModelStore()
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthDatabase::class.java).build()
        model = HealthViewModel(HealthRepository(db.records()))
        store.put("search", model)
    }
    @After fun close() { store.clear(); db.close() }
    private fun insert(date: String, steps: Int, calories: Int, water: Int): Long = runBlocking {
        db.records().insert(HealthRecord(date = date, steps = steps, calories = calories, water = water))
    }
    private fun launch() {
        compose.setContent { HealthMateTheme { HealthMateApp(model) } }
        compose.onNodeWithContentDescription("Search records").performClick()
        compose.waitUntil(10_000) { model.searchOpen.value }
    }
    @Test fun searchRequiresADateAndShowsResultsWithEditAndDelete() {
        val today = LocalDate.now().toString()
        val id = insert(today, 1000, 50, 500)
        launch()
        compose.onNodeWithTag("search-submit").assertExists()
        compose.onNodeWithTag("search-submit").performClick()
        compose.runOnIdle { assertNull(model.searchQuery.value) }
        compose.runOnIdle { model.pickSearchDate(today) }
        compose.onNodeWithTag("search-submit").performClick()
        compose.waitUntil(10_000) { model.searchResults.value.isNotEmpty() }
        compose.onNodeWithText("Daily health record").assertExists()
        compose.onNodeWithText("Steps walked").assertExists()
        compose.onNodeWithTag("search-edit-$id").performClick()
        compose.waitUntil(10_000) { model.editor.value.open && !model.editor.value.loading }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(10_000) { !model.editor.value.open && model.searchOpen.value }
        compose.onNodeWithTag("search-delete-$id").performClick()
        compose.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertNull(model.deleteTarget.value) }
    }
    @Test fun searchEmptyStateAppearsForMissingDateAndShowAllOpensRecords() {
        insert("2000-01-01", 10, 1, 10)
        launch()
        compose.runOnIdle { model.pickSearchDate(LocalDate.now().toString()) }
        compose.onNodeWithTag("search-submit").performClick()
        compose.waitUntil(10_000) { model.searchQuery.value != null && model.searchResults.value.isEmpty() }
        compose.onNodeWithTag("search-empty").assertExists()
        compose.onNodeWithText("No records found").assertExists()
        compose.onNodeWithTag("search-show-all").performClick()
        compose.waitUntil(10_000) { !model.searchOpen.value && model.tab.value == "records" }
        compose.onNodeWithText("Your health history").assertExists()
    }
    @Test fun dashboardViewingChipShowsSelectedDayAndClearsBackToToday() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        insert(yesterday, 2000, 100, 750)
        compose.setContent { HealthMateTheme { HealthMateApp(model) } }
        compose.waitUntil(10_000) { !model.totals.value.loading }
        compose.runOnIdle { model.viewDate(yesterday) }
        compose.waitUntil(10_000) { model.totals.value.date == yesterday }
        compose.onNodeWithTag("view-date-chip").performScrollTo().assertIsDisplayed()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("750 ml").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("view-date-chip").performClick()
        compose.waitUntil(10_000) { model.viewDate.value == null && model.totals.value.date == LocalDate.now().toString() }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("0 ml").fetchSemanticsNodes().isNotEmpty() }
    }
}

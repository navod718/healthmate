package com.navod718.healthmate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
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
class RecordsUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: HealthDatabase
    private lateinit var model: HealthViewModel
    private val store = ViewModelStore()
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthDatabase::class.java).build()
        model = HealthViewModel(HealthRepository(db.records()))
        store.put("records", model)
    }
    @After fun close() { store.clear(); db.close() }
    private fun insert(date: String = LocalDate.now().toString(), water: Int = 500): Long = runBlocking {
        db.records().insert(HealthRecord(date = date, steps = 1000, calories = 50, water = water))
    }
    private fun launchRecords() {
        compose.setContent { HealthMateTheme { HealthMateApp(model) } }
        compose.onNodeWithText("Records").performClick()
        compose.waitUntil(10_000) { !model.records.value.loading }
    }
    private fun waitForRecord(id: Long) {
        compose.waitUntil(10_000) { model.records.value.records.any { it.id == id } }
    }
    @Test fun editChangesTheExistingRecordAndDashboard() {
        val id = insert()
        launchRecords(); waitForRecord(id)
        compose.onNodeWithTag("edit-$id").performScrollTo().performClick()
        compose.waitUntil(10_000) { model.editor.value.open && !model.editor.value.loading }
        compose.onNodeWithText("Steps walked").performScrollTo().performTextReplacement("-1")
        compose.onNodeWithText("Save changes").performScrollTo().performClick()
        compose.onNodeWithText("Enter a whole number from 0 to 2,147,483,647.").assertExists()
        compose.onNodeWithText("Steps walked").performScrollTo().performTextReplacement("1500")
        compose.onNodeWithText("Water intake").performScrollTo().performTextReplacement("750")
        compose.onNodeWithText("Save changes").performScrollTo().performClick()
        compose.waitUntil(10_000) { !model.editor.value.open }
        assertEquals(1, runBlocking { db.records().count() })
        assertEquals(1500, runBlocking { db.records().find(id) }?.steps)
        compose.onNodeWithText("Dashboard").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("750 ml").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("750 ml").performScrollTo().assertIsDisplayed()
    }
    @Test fun deletionCanBeCancelledThenConfirmedAndTotalsRefresh() {
        val id = insert()
        launchRecords(); waitForRecord(id)
        compose.onNodeWithTag("delete-$id").performScrollTo().performClick()
        compose.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
        assertEquals(1, runBlocking { db.records().count() })
        compose.onNodeWithTag("delete-$id").performScrollTo().performClick()
        compose.onNodeWithText("Delete record", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) { model.records.value.totalCount == 0 && model.records.value.records.isEmpty() && !model.records.value.loading && model.deleteTarget.value == null }
        compose.onNodeWithText("No records yet").assertExists()
        compose.onNodeWithText("Dashboard").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("0 ml").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("0 ml").performScrollTo().assertIsDisplayed()
    }
    @Test fun calendarFilterClearAndNoResultsWorkWithoutOfferingSamplesOverExistingData() {
        val todayId = insert()
        val olderId = insert(LocalDate.now().minusDays(1).toString())
        launchRecords()
        compose.waitUntil(10_000) { model.records.value.records.size == 2 }
        compose.onNodeWithText("Filter by date").performClick()
        onView(withId(android.R.id.button1)).perform(click())
        compose.waitUntil(10_000) { !model.records.value.loading && model.records.value.records.size == 1 }
        compose.onNodeWithTag("record-$todayId").assertExists()
        compose.onNodeWithTag("record-$olderId").assertDoesNotExist()
        compose.onNodeWithText("Clear").performClick()
        compose.waitUntil(10_000) { model.records.value.records.size == 2 }
        compose.runOnIdle { model.filterByDate("2000-01-01") }
        compose.waitUntil(10_000) { !model.records.value.loading && model.records.value.records.isEmpty() }
        compose.onNodeWithText("No records for this date").assertExists()
        compose.onNodeWithText("Try sample records").assertDoesNotExist()
    }
    @Test fun sampleLoadingRequiresConfirmationAndOnlyRunsOnAnEmptyDatabase() {
        launchRecords()
        compose.onNodeWithText("Try sample records").performClick()
        compose.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
        assertEquals(0, runBlocking { db.records().count() })
        compose.onNodeWithText("Try sample records").performClick()
        compose.onNodeWithText("Load samples", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) { model.records.value.totalCount == 4 && model.records.value.records.size == 4 }
        assertFalse(runBlocking { HealthRepository(db.records()).loadSamples() })
        compose.onNodeWithText("Dashboard").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("500 ml").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("500 ml").performScrollTo().assertIsDisplayed()
    }
}

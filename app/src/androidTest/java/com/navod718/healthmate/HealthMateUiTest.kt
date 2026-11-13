package com.navod718.healthmate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.navod718.healthmate.core.database.HealthDatabase
import com.navod718.healthmate.feature.health.data.HealthRepository
import com.navod718.healthmate.feature.health.presentation.HealthMateApp
import com.navod718.healthmate.feature.health.presentation.HealthViewModel
import com.navod718.healthmate.ui.theme.HealthMateTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthMateUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: HealthDatabase
    private lateinit var model: HealthViewModel
    private val store = ViewModelStore()
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthDatabase::class.java).build()
        model = HealthViewModel(HealthRepository(database.records()))
        store.put("health", model)
        compose.setContent {
            HealthMateTheme { HealthMateApp(model) }
        }
    }
    @After fun close() { store.clear(); database.close() }

    @Test fun dashboardContentScrollsAboveAddRecordButton() {
        val footer = "Totals include every record you log for today. Your records stay on this device."
        compose.waitUntil(10_000) { compose.onAllNodesWithText(footer).fetchSemanticsNodes().isNotEmpty() }
        val viewport = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
        val addButton = compose.onNode(
            hasClickAction() and hasAnyDescendant(hasText("Add record")),
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        assertTrue("The dashboard viewport must end above the Add record button", viewport.bottom <= addButton.top)
        compose.onNodeWithText(footer).performScrollTo().assertIsDisplayed()
    }

    @Test fun addFormValidatesThenPersistsAndDisplaysRecord() {
        compose.onNodeWithText("Add record", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Save record").performScrollTo().performClick()
        compose.onAllNodesWithText("This field is required.").assertCountEquals(3)
        compose.onNodeWithText("Steps walked").performScrollTo().performTextInput("1200")
        compose.onAllNodesWithText("This field is required.").assertCountEquals(2)
        compose.onNodeWithText("Steps walked").performImeAction()
        compose.onNodeWithText("Calories burned").assertIsFocused().performTextInput("60")
        compose.onNodeWithText("Calories burned").performImeAction()
        compose.onNodeWithText("Water intake").assertIsFocused().performTextInput("500")
        compose.onNodeWithText("Water intake").performImeAction()
        compose.onNodeWithText("Water intake").assertIsNotFocused()
        compose.onNodeWithText("Save record").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { database.records().observeTotals(LocalDate.now().toString()).first().water == 500L } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("500 ml").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("500 ml").performScrollTo().assertIsDisplayed()
    }
    @Test fun backFromFormDoesNotSaveAndEmptyDashboardShowsZero() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("0 ml").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("0 ml").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Add record", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Steps walked").performScrollTo().performTextInput("500")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("500", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard", useUnmergedTree = true).performClick()
        compose.onNodeWithText("0 steps").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0 ml").performScrollTo().assertIsDisplayed()
    }
    @Test fun goalsCanBeChangedAndProgressReflectsNewTarget() {
        compose.onNodeWithContentDescription("Daily goals").performClick()
        compose.onNodeWithText("Water goal (ml)").performScrollTo().performTextReplacement("0")
        compose.onNodeWithText("Save goals", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Each goal must be a positive whole number up to 2,147,483,647.").assertExists()
        compose.onNodeWithText("Water goal (ml)").performScrollTo().performTextReplacement("1000")
        compose.onNodeWithText("Save goals", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("0% of 1,000 ml goal").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("0% of 1,000 ml goal").performScrollTo().assertIsDisplayed()
    }
}

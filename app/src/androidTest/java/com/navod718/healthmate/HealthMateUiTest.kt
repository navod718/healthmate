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
    @Test fun addFormValidatesThenPersistsAndDisplaysRecord() {
        compose.onNodeWithText("Add record").performClick()
        compose.onNodeWithText("Save record").performScrollTo().performClick()
        compose.onAllNodesWithText("This field is required.").assertCountEquals(3)
        compose.onNodeWithText("Steps walked").performScrollTo().performTextInput("1200")
        compose.onNodeWithText("Calories burned").performScrollTo().performTextInput("60")
        compose.onNodeWithText("Water intake").performScrollTo().performTextInput("500")
        compose.onNodeWithText("Save record").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { database.records().observeTotals(LocalDate.now().toString()).first().water == 500L } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("500 ml").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("500 ml").performScrollTo().assertIsDisplayed()
    }
    @Test fun backFromFormDoesNotSaveAndEmptyDashboardShowsZero() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("0 ml").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("0 ml").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Add record").performClick()
        compose.onNodeWithText("Steps walked").performScrollTo().performTextInput("500")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("0 steps").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0 ml").performScrollTo().assertIsDisplayed()
    }
}

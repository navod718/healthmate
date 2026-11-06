package com.navod718.healthmate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun activityRecreationPreservesUnsavedFormAndDiscardProtection() {
        compose.onNodeWithText("Add record", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Steps walked").performScrollTo().performTextInput("2345")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("2345", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Keep editing", useUnmergedTree = true).performClick()
        compose.onNodeWithText("2345", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Your day, at a glance").assertIsDisplayed()
    }
}

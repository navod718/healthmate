package com.navod718.healthmate

import android.content.Context
import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.filters.SdkSuppress
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.runner.RunWith

// Report screenshots use an in-memory database and sample records.
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class ReportScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: HealthDatabase
    private lateinit var model: HealthViewModel
    private val store = ViewModelStore()
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthDatabase::class.java).build()
        runBlocking {
            db.records().insert(HealthRecord(date=LocalDate.now().toString(), steps=1200, calories=60, water=500))
            db.records().insert(HealthRecord(date=LocalDate.now().minusDays(1).toString(), steps=6400, calories=310, water=1800))
        }
        model = HealthViewModel(HealthRepository(db.records())); store.put("report",model)
        compose.setContent { HealthMateTheme { HealthMateApp(model) } }
    }
    @After fun close() { store.clear(); db.close() }
    private fun capture(name: String) {
        compose.runOnIdle { model.clearMessage() }
        compose.waitForIdle()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val image = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"$name.png")
            put(MediaStore.Images.Media.MIME_TYPE,"image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/HealthMateReport")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
        context.contentResolver.openOutputStream(uri)!!.use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
        values.clear(); values.put(MediaStore.Images.Media.IS_PENDING,0)
        context.contentResolver.update(uri,values,null,null)
        image.recycle()
    }
    @Test fun captureImplementedScreensWithSyntheticRecords() {
        compose.waitUntil(10_000) { !model.totals.value.loading && model.totals.value.totals.water == 500L }
        capture("01-dashboard")
        compose.onNodeWithText("25% of 2,000 ml goal").performScrollTo()
        capture("09-dashboard-totals")
        compose.onNodeWithText("Add record",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Save record").performScrollTo().performClick()
        compose.onAllNodesWithText("This field is required.").assertCountEquals(3)
        capture("02-validation")
        compose.onNodeWithText("Steps walked").performScrollTo().performTextInput("300")
        compose.onNodeWithText("Calories burned").performScrollTo().performTextInput("20")
        compose.onNodeWithText("Water intake").performScrollTo().performTextInput("250")
        compose.onNodeWithText("Water intake").performImeAction()
        compose.onNodeWithText("Log a healthy moment").performScrollTo()
        capture("03-add-form")
        compose.onNodeWithText("Save record").performScrollTo().performClick()
        compose.waitUntil(10_000) { !model.editor.value.open && model.records.value.totalCount == 3 }
        compose.onNodeWithText("Records").performClick()
        compose.waitUntil(10_000) { !model.records.value.loading && model.records.value.records.size == 3 }
        capture("04-history")
        compose.onNodeWithText("Filter by date").performClick()
        onView(withId(android.R.id.button1)).perform(click())
        compose.waitUntil(10_000) { !model.records.value.loading && model.records.value.records.size == 2 }
        capture("05-date-filter")
        compose.onNodeWithTag("edit-3").performScrollTo().performClick()
        compose.waitUntil(10_000) { model.editor.value.open && !model.editor.value.loading }
        compose.onNodeWithText("Update your activity").performScrollTo()
        capture("06-edit-form")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("delete-3").performScrollTo().performClick()
        capture("07-delete-confirmation")
        compose.onNodeWithText("Cancel",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Dashboard").performClick()
        compose.onNodeWithContentDescription("Daily goals").performClick()
        capture("08-goals")
    }
}

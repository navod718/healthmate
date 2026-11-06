package com.navod718.healthmate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.feature.health.data.*
import com.navod718.healthmate.feature.health.presentation.HealthViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DraftAndGoalsTest {
    private val store = ViewModelStore()
    private var count = 0
    private val dao = object : HealthRecordDao {
        override fun observeTotals(date: String) = flowOf(DailyTotals())
        override fun observeGoals(): Flow<DailyGoals?> = flowOf(null)
        override suspend fun saveGoals(goals: DailyGoals) = Unit
        override suspend fun insert(record: HealthRecord) = 1L
    }
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun close() { store.clear(); Dispatchers.resetMain() }
    private fun model(handle: SavedStateHandle = SavedStateHandle()): HealthViewModel =
        HealthViewModel(HealthRepository(dao), handle).also { store.put("model-${count++}", it) }

    @Test fun dirtyFormRequiresConfirmationAndKeepEditingRetainsValues() {
        val model = model()
        model.add()
        model.change(RecordInput(steps = "1200", calories = "60", water = "500"))
        model.closeEditor()
        assertTrue(model.discardPrompt.value)
        assertTrue(model.editor.value.open)
        model.keepEditing()
        assertFalse(model.discardPrompt.value)
        assertEquals("1200", model.editor.value.input.steps)
        model.closeEditor()
        model.discard()
        assertFalse(model.editor.value.open)
    }
    @Test fun untouchedFormClosesWithoutPrompt() {
        val model = model()
        model.add(); model.closeEditor()
        assertFalse(model.editor.value.open)
        assertFalse(model.discardPrompt.value)
    }
    @Test fun draftRestoresFromSavedStateAndStillRequiresDiscardConfirmation() {
        val handle = SavedStateHandle()
        val original = model(handle)
        original.add()
        original.change(RecordInput("2026-11-06", "1234", "50", "700"))
        val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
        val restored = model(restoredHandle)
        assertEquals(original.editor.value.input, restored.editor.value.input)
        assertTrue(restored.editor.value.open)
        restored.closeEditor()
        assertTrue(restored.discardPrompt.value)
    }
    @Test fun goalsArePositiveAndProgressIsCappedWithoutChangingTotals() {
        assertTrue(DailyGoals().isValid())
        assertFalse(DailyGoals(water = 0).isValid())
        assertFalse(DailyGoals(steps = -1).isValid())
        assertEquals(0f, DailyGoals().progress(0,2000), 0f)
        assertEquals(.5f, DailyGoals().progress(1000,2000), 0f)
        assertEquals(1f, DailyGoals().progress(3000,2000), 0f)
    }
}

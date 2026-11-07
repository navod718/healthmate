package com.navod718.healthmate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.feature.health.data.*
import com.navod718.healthmate.feature.health.presentation.HealthViewModel
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RecordsViewModelTest {
    private val store = ViewModelStore()
    private var counter = 0
    private val dao = object : HealthRecordDao {
        val entry = MutableStateFlow<HealthRecord?>(HealthRecord(7, LocalDate.now().toString(), 1000, 50, 500))
        var inserts = 0
        var updates = 0
        var deletes = 0
        var failDelete = false
        override fun observeTotals(date: String) = entry.map { if (it?.date == date) DailyTotals(it.steps.toLong(), it.calories.toLong(), it.water.toLong()) else DailyTotals() }
        override fun observeGoals(): Flow<DailyGoals?> = flowOf(null)
        override suspend fun saveGoals(goals: DailyGoals) = Unit
        override suspend fun insert(record: HealthRecord): Long { inserts++; return 8L }
        override fun observeRecords(date: String?) = entry.map { if (it != null && (date == null || it.date == date)) listOf(it) else emptyList() }
        override fun observeCount() = entry.map { if (it == null) 0 else 1 }
        override suspend fun count() = if (entry.value == null) 0 else 1
        override suspend fun find(id: Long) = entry.value?.takeIf { it.id == id }
        override suspend fun update(record: HealthRecord): Int {
            updates++
            if (entry.value?.id != record.id) return 0
            entry.value = record; return 1
        }
        override suspend fun delete(id: Long): Int {
            deletes++
            if (failDelete) error("Simulated database error")
            if (entry.value?.id != id) return 0
            entry.value = null; return 1
        }
    }
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun close() { store.clear(); Dispatchers.resetMain() }
    private fun model(handle: SavedStateHandle = SavedStateHandle()) = HealthViewModel(HealthRepository(dao), handle).also { store.put("model-${counter++}", it) }

    @Test fun editingUpdatesExistingIdWithoutInsertingAndUntouchedEditCloses() {
        val model = model()
        model.edit(7)
        assertEquals("1000", model.editor.value.input.steps)
        model.closeEditor()
        assertFalse(model.discardPrompt.value)
        assertFalse(model.editor.value.open)
        model.edit(7)
        model.change(model.editor.value.input.copy(steps = "1500"))
        model.save()
        assertEquals(7L, dao.entry.value?.id)
        assertEquals(1500, dao.entry.value?.steps)
        assertEquals(1, dao.updates)
        assertEquals(0, dao.inserts)
        assertFalse(model.editor.value.open)
    }
    @Test fun editDraftRestoresItsIdAndOriginalValues() {
        val handle = SavedStateHandle()
        val first = model(handle)
        first.selectTab("records"); first.filterByDate(LocalDate.now().toString())
        first.edit(7)
        first.change(first.editor.value.input.copy(water = "750"))
        val restored = model(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertEquals(7L, restored.editor.value.id)
        assertEquals("500", restored.editor.value.original.water)
        assertEquals("750", restored.editor.value.input.water)
        assertEquals("records", restored.tab.value)
        assertEquals(LocalDate.now().toString(), restored.filter.value)
        restored.closeEditor(); assertTrue(restored.discardPrompt.value)
        restored.keepEditing(); restored.save()
        assertEquals(750, dao.entry.value?.water)
        assertEquals(0, dao.inserts)
    }
    @Test fun missingRecordsAreHandledWithoutRecreatingDeletedData() {
        val model = model()
        model.edit(999)
        assertFalse(model.editor.value.open)
        assertEquals("This record no longer exists.", model.message.value)
        model.edit(7)
        dao.entry.value = null
        model.change(model.editor.value.input.copy(water = "900"))
        model.save()
        assertTrue(model.editor.value.open)
        assertNotNull(model.editor.value.errors["form"])
        assertEquals("900", model.editor.value.input.water)
        assertEquals(0, dao.inserts)
    }
    @Test fun addingFromFilteredHistoryUsesTheSelectedDate() {
        val model = model()
        val yesterday = LocalDate.now().minusDays(1).toString()
        model.selectTab("records"); model.filterByDate(yesterday); model.add()
        assertEquals(yesterday, model.editor.value.input.date)
        model.closeEditor()
        assertFalse(model.discardPrompt.value)
        assertFalse(model.editor.value.open)
    }
    @Test fun deleteRequiresConfirmationAndFailureCanBeRetried() {
        val model = model()
        model.requestDelete(dao.entry.value!!)
        model.cancelDelete()
        assertEquals(0, dao.deletes)
        model.requestDelete(dao.entry.value!!)
        dao.failDelete = true
        model.confirmDelete()
        assertNotNull(model.deleteError.value)
        assertNotNull(model.deleteTarget.value)
        assertNotNull(dao.entry.value)
        dao.failDelete = false
        model.confirmDelete()
        assertNull(model.deleteTarget.value)
        assertNull(dao.entry.value)
        assertFalse(model.busy.value)
    }
}

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
        var failGoalsRead = false
        var goalsReads = 0
        var failBreakdown = false
        var failInsert = false
        var lastInserted: HealthRecord? = null
        val savedGoals = MutableStateFlow<DailyGoals?>(null)
        var filteredReadGate: CompletableDeferred<Unit>? = null
        override fun observeTotals(date: String) = entry.map { if (it?.date == date) DailyTotals(it.steps.toLong(), it.calories.toLong(), it.water.toLong()) else DailyTotals() }
        override fun observeGoals(): Flow<DailyGoals?> = flow {
            goalsReads++
            if (failGoalsRead) error("Simulated goals read failure")
            emitAll(savedGoals)
        }
        override suspend fun saveGoals(goals: DailyGoals) { savedGoals.value = goals }
        override suspend fun insert(record: HealthRecord): Long {
            inserts++
            if (failInsert) error("Simulated insert failure")
            lastInserted = record; return 8L
        }
        override fun observeRecords(date: String?) = flow {
            if (date != null) filteredReadGate?.await()
            emitAll(entry.map { if (it != null && (date == null || it.date == date)) listOf(it) else emptyList() })
        }
        override fun observeCount() = entry.map { if (it == null) 0 else 1 }
        override fun observeDailyBreakdown(): Flow<List<DailyBreakdown>> = flow {
            if (failBreakdown) error("Simulated breakdown read failure")
            emitAll(entry.map { if (it == null) emptyList() else listOf(DailyBreakdown(it.date, it.steps.toLong(), it.calories.toLong(), it.water.toLong())) })
        }
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

    @Test fun editingOneFieldRetainsUnrelatedValidationErrors() {
        val model = model()
        model.add()
        model.save()
        assertEquals(setOf("steps", "calories", "water"), model.editor.value.errors.keys)

        model.change(model.editor.value.input.copy(steps = "125"))
        assertEquals(setOf("calories", "water"), model.editor.value.errors.keys)
        model.change(model.editor.value.input.copy(calories = "-1"))
        assertEquals("Enter a whole number from 0 to 2,147,483,647.", model.editor.value.errors["calories"])
        assertEquals("This field is required.", model.editor.value.errors["water"])
        assertEquals(0, dao.inserts)
    }

    @Test fun goalObservationCanRetryAndTracksLaterWrites() = runTest {
        val model = model()
        dao.failGoalsRead = true
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.goals.collect() }
        runCurrent()
        assertTrue(model.goals.value.loadFailed)
        assertEquals(DailyGoals(), model.goals.value.value)

        val original = DailyGoals(water = 2400)
        dao.savedGoals.value = original
        dao.failGoalsRead = false
        model.retryGoals()
        runCurrent()
        assertFalse(model.goals.value.loadFailed)
        assertEquals(original, model.goals.value.value)

        val updated = original.copy(water = 2600)
        model.saveGoals(updated)
        runCurrent()
        assertEquals(updated, model.goals.value.value)
        assertEquals(2, dao.goalsReads)
    }

    @Test fun savingGoalsRestartsFailedObservation() = runTest {
        val model = model()
        dao.failGoalsRead = true
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.goals.collect() }
        runCurrent()
        assertTrue(model.goals.value.loadFailed)

        val updated = DailyGoals(steps = 6000)
        model.saveGoals(updated)
        runCurrent()
        assertTrue(model.goals.value.loadFailed)
        assertEquals(updated, model.goals.value.value)
        assertEquals(2, dao.goalsReads)

        dao.failGoalsRead = false
        model.retryGoals()
        runCurrent()
        assertFalse(model.goals.value.loadFailed)
        assertEquals(updated, model.goals.value.value)
    }

    @Test fun pendingFilterNeverPairsItsDateWithPreviousRows() = runTest {
        val model = model()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.records.collect() }
        runCurrent()
        assertNull(model.records.value.date)
        assertEquals(listOf(dao.entry.value), model.records.value.records)

        val gate = CompletableDeferred<Unit>()
        dao.filteredReadGate = gate
        val yesterday = LocalDate.now().minusDays(1).toString()
        model.filterByDate(yesterday)
        runCurrent()
        assertEquals(yesterday, model.records.value.date)
        assertTrue(model.records.value.loading)
        assertTrue(model.records.value.records.isEmpty())

        gate.complete(Unit)
        runCurrent()
        assertEquals(yesterday, model.records.value.date)
        assertFalse(model.records.value.loading)
        assertTrue(model.records.value.records.isEmpty())
    }

    @Test fun insightsDeriveStreaksAndAchievementsFromRecords() = runTest {
        val model = model()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.insights.collect() }
        runCurrent()
        assertFalse(model.insights.value.loading)
        assertNull(model.insights.value.error)
        assertEquals(1, model.insights.value.breakdown.size)
        assertEquals(0, model.insights.value.streak)
        assertTrue(model.insights.value.achievements.first { it.id == "first-record" }.unlocked)
        assertFalse(model.insights.value.achievements.first { it.id == "perfect-day" }.unlocked)
    }

    @Test fun quickAddWaterInsertsAWaterOnlyRecordForToday() {
        val model = model()
        model.quickAddWater(250)
        assertEquals(1, dao.inserts)
        assertEquals(250, dao.lastInserted?.water)
        assertEquals(0, dao.lastInserted?.steps)
        assertEquals(0, dao.lastInserted?.calories)
        assertEquals(LocalDate.now().toString(), dao.lastInserted?.date)
        assertEquals("Added 250 ml water.", model.message.value)
        assertFalse(model.busy.value)
        assertFalse(model.editor.value.open)
    }
    @Test fun quickAddWaterReportsFailureAndReleasesBusy() {
        val model = model()
        dao.failInsert = true
        model.quickAddWater(500)
        assertEquals(1, dao.inserts)
        assertNull(dao.lastInserted)
        assertEquals("Could not add water. Please try again.", model.message.value)
        assertFalse(model.busy.value)
    }
    @Test fun searchFlowOpensSubmitsShowsAllAndCloses() = runTest {
        val model = model()
        val today = LocalDate.now().toString()
        model.submitSearch()
        assertNull(model.searchQuery.value)
        model.openSearch()
        assertTrue(model.searchOpen.value)
        model.pickSearchDate(today)
        assertEquals(today, model.searchDate.value)
        model.submitSearch()
        assertEquals(today, model.searchQuery.value)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.searchResults.collect() }
        runCurrent()
        assertEquals(listOf(dao.entry.value), model.searchResults.value)
        model.showAllRecords()
        assertFalse(model.searchOpen.value)
        assertNull(model.searchDate.value)
        assertNull(model.searchQuery.value)
        assertEquals("records", model.tab.value)
        assertNull(model.filter.value)
    }
    @Test fun dashboardViewDateSwitchesTotalsAndRestoresAcrossRecreation() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val model = model()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.totals.collect() }
        runCurrent()
        assertEquals(LocalDate.now().toString(), model.totals.value.date)
        model.viewDate(yesterday)
        runCurrent()
        assertEquals(yesterday, model.totals.value.date)
        assertEquals(yesterday, model.viewDate.value)
        val restoredHandle = SavedStateHandle(mapOf("viewDate" to yesterday))
        val restored = HealthViewModel(HealthRepository(dao), restoredHandle).also { store.put("restored-${counter++}", it) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { restored.totals.collect() }
        runCurrent()
        assertEquals(yesterday, restored.viewDate.value)
        assertEquals(yesterday, restored.totals.value.date)
        restored.clearViewDate()
        runCurrent()
        assertNull(restored.viewDate.value)
        assertEquals(LocalDate.now().toString(), restored.totals.value.date)
    }
    @Test fun insightsErrorRecoversWithRetry() = runTest {
        val model = model()
        dao.failBreakdown = true
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.insights.collect() }
        runCurrent()
        assertEquals("Could not load insights. Please retry.", model.insights.value.error)
        assertTrue(model.insights.value.breakdown.isEmpty())
        dao.failBreakdown = false
        model.retry()
        runCurrent()
        assertNull(model.insights.value.error)
        assertFalse(model.insights.value.loading)
        assertEquals(1, model.insights.value.breakdown.size)
    }
}

package com.navod718.healthmate

import androidx.lifecycle.ViewModelStore
import com.navod718.healthmate.feature.health.data.*
import com.navod718.healthmate.feature.health.presentation.HealthViewModel
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class QuickWaterViewModelTest {
    private val store = ViewModelStore()
    private val dao = object : HealthRecordDao {
        val logged = mutableListOf<HealthRecord>()
        var pending: CompletableDeferred<Unit>? = null
        var fail = false
        var attempts = 0
        override fun observeTotals(date: String) = flowOf(DailyTotals())
        override fun observeDailyActivity(start: String, end: String): Flow<List<DailyActivity>> = flowOf(emptyList())
        override fun observeGoals(): Flow<DailyGoals?> = flowOf(null)
        override suspend fun saveGoals(goals: DailyGoals) = Unit
        override suspend fun insert(record: HealthRecord): Long {
            attempts++; pending?.await()
            if (fail) error("Simulated write failure")
            logged += record; return logged.size.toLong()
        }
        override fun observeRecords(date: String?): Flow<List<HealthRecord>> = flowOf(emptyList())
        override fun observeCount() = flowOf(0)
        override suspend fun count() = 0
        override suspend fun find(id: Long): HealthRecord? = null
        override suspend fun update(record: HealthRecord) = 0
        override suspend fun delete(id: Long) = 0
    }
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun close() { store.clear(); Dispatchers.resetMain() }
    private fun model() = HealthViewModel(HealthRepository(dao)).also { store.put("water", it) }

    @Test fun supportedTapsCreateTodayWaterOnlyRecords() {
        val model = model()
        model.quickWater(250); model.quickWater(500)
        assertEquals(listOf(250,500), dao.logged.map { it.water })
        assertTrue(dao.logged.all { it.date == LocalDate.now().toString() && it.steps == 0 && it.calories == 0 })
        assertFalse(model.busy.value)
    }
    @Test fun tapsDuringAnOutstandingWriteDoNotDuplicateRecords() {
        val model = model()
        dao.pending = CompletableDeferred()
        model.quickWater(250); model.quickWater(500)
        assertTrue(model.busy.value)
        assertEquals(1, dao.attempts)
        dao.pending!!.complete(Unit)
        assertEquals(1, dao.logged.size)
        assertFalse(model.busy.value)
    }
    @Test fun failureAllowsRetryAndDoesNotLeaveTheControlsBusy() {
        val model = model()
        dao.fail = true; model.quickWater(250)
        assertTrue(dao.logged.isEmpty())
        assertTrue(model.message.value!!.contains("Could not log water"))
        assertFalse(model.busy.value)
        dao.fail = false; model.quickWater(500)
        assertEquals(500, dao.logged.single().water)
    }
    @Test fun invalidAmountsAndCallsDuringEditingAreIgnored() {
        val model = model()
        model.quickWater(125)
        model.add(); model.quickWater(250)
        assertEquals(0, dao.attempts)
        assertTrue(model.editor.value.open)
    }
    @Test(expected = IllegalArgumentException::class)
    fun repositoryRejectsUnsupportedAmounts() = runBlocking { HealthRepository(dao).addWater(100) }
}

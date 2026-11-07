package com.navod718.healthmate

import android.content.Context
import java.util.UUID
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.navod718.healthmate.core.database.HealthDatabase
import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.feature.health.data.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordsDatabaseTest {
    private lateinit var db: HealthDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthDatabase::class.java).build() }
    @After fun close() { db.close() }

    @Test fun historyFilteringEditsAndDeletionKeepTotalsConsistent() = runBlocking {
        val dao = db.records()
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val first = dao.insert(HealthRecord(date = today, steps = 1000, calories = 50, water = 500))
        val second = dao.insert(HealthRecord(date = today, steps = 500, calories = 25, water = 250))
        val older = dao.insert(HealthRecord(date = yesterday, steps = 100, calories = 5, water = 50))
        assertEquals(listOf(second, first, older), dao.observeRecords(null).first().map { it.id })
        assertEquals(listOf(second, first), dao.observeRecords(today).first().map { it.id })
        assertTrue(dao.observeRecords("2000-01-01").first().isEmpty())
        val repository = HealthRepository(dao)
        assertTrue(repository.update(first, RecordInput(yesterday, "2000", "100", "1000")))
        assertEquals(3, dao.count())
        assertEquals(yesterday, dao.find(first)?.date)
        assertEquals(DailyTotals(500,25,250), dao.observeTotals(today).first())
        assertEquals(DailyTotals(2100,105,1050), dao.observeTotals(yesterday).first())
        assertTrue(repository.delete(second))
        assertEquals(DailyTotals(), dao.observeTotals(today).first())
        assertNull(dao.find(second))
        assertFalse(repository.delete(second))
        assertFalse(repository.update(second, RecordInput(today, "1", "1", "1")))
        assertEquals(2, dao.count())
    }
    @Test fun concurrentSampleRequestsOnlyPopulateAnEmptyDatabaseOnce() = runBlocking {
        val repository = HealthRepository(db.records())
        val results = listOf(async(Dispatchers.IO) { repository.loadSamples() }, async(Dispatchers.IO) { repository.loadSamples() }).awaitAll()
        assertEquals(1, results.count { it })
        assertEquals(4, db.records().count())
        val before = db.records().observeRecords(null).first()
        assertFalse(repository.loadSamples())
        assertEquals(before, db.records().observeRecords(null).first())
        assertEquals(DailyTotals(1500,75,500), db.records().observeTotals(LocalDate.now().toString()).first())
    }
    @Test fun editsAndDeletesSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "healthmate-records-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, HealthDatabase::class.java, name).build()
        var persistent = open()
        val today = LocalDate.now().toString()
        try {
            val id = persistent.records().insert(HealthRecord(date = today, steps = 1000, calories = 50, water = 500))
            assertTrue(HealthRepository(persistent.records()).update(id, RecordInput(today, "2000", "100", "1000")))
            persistent.close(); persistent = open()
            assertEquals(HealthRecord(id, today, 2000, 100, 1000), persistent.records().find(id))
            assertTrue(HealthRepository(persistent.records()).delete(id))
            persistent.close(); persistent = open()
            assertNull(persistent.records().find(id))
            assertEquals(0, persistent.records().count())
        } finally { persistent.close(); context.deleteDatabase(name) }
    }
}

package com.navod718.healthmate

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.navod718.healthmate.core.database.HealthDatabase
import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.feature.health.data.*
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeeklyActivityTest {
    private lateinit var db: HealthDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthDatabase::class.java).build() }
    @After fun close() { db.close() }
    @Test fun weeklyRangeGroupsEntriesFillsGapsAndHandlesYearBoundariesAndLargeTotals() = runBlocking {
        val dao = db.records()
        val end = LocalDate.of(2026,1,3)
        val start = end.minusDays(6)
        dao.insert(HealthRecord(date = start.toString(), steps = 1000, calories = 50, water = 250))
        dao.insert(HealthRecord(date = start.toString(), steps = 500, calories = 25, water = 250))
        repeat(2) { dao.insert(HealthRecord(date = end.toString(), steps = Int.MAX_VALUE, calories = 1, water = 250)) }
        dao.insert(HealthRecord(date = start.minusDays(1).toString(), steps = 999, calories = 999, water = 999))
        dao.insert(HealthRecord(date = end.plusDays(1).toString(), steps = 999, calories = 999, water = 999))
        val days = HealthRepository(dao).observeWeek(end.toString()).first()
        assertEquals((0L..6L).map { start.plusDays(it).toString() }, days.map { it.date })
        assertEquals(DailyActivity(start.toString(),1500,75,500), days.first())
        assertEquals(DailyActivity(start.plusDays(1).toString()), days[1])
        assertEquals(Int.MAX_VALUE.toLong()*2, days.last().steps)
        assertEquals(1000L, days.sumOf { it.water })
    }
    @Test fun quickWaterUpdatesDailyWeeklyAndHistoryAndRespondsToEditsAndDeletion() = runBlocking {
        val repository = HealthRepository(db.records())
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        repository.addWater(250); repository.addWater(500)
        assertEquals(DailyTotals(0,0,750), repository.observeTotals(today).first())
        assertEquals(750L, repository.observeWeek(today).first().last().water)
        val records = repository.observeRecords(null).first()
        assertEquals(2, records.size)
        assertTrue(records.all { it.steps == 0 && it.calories == 0 })
        assertTrue(repository.update(records.first().id, RecordInput(yesterday,"0","0","1000")))
        val changed = repository.observeWeek(today).first()
        assertEquals(250L, changed.last().water)
        assertEquals(1000L, changed[5].water)
        assertTrue(repository.delete(records.last().id))
        assertEquals(0L, repository.observeWeek(today).first().last().water)
    }
}

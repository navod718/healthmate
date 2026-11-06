package com.navod718.healthmate

import android.content.Context
import java.util.UUID
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.navod718.healthmate.core.database.HealthDatabase
import com.navod718.healthmate.feature.health.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthDatabaseTest {
    private lateinit var db: HealthDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthDatabase::class.java).build() }
    @After fun close() { db.close() }
    @Test fun insertsAggregateOnlyTheMatchingDay() = runBlocking {
        val dao = db.records()
        val record = HealthRecord(date = "2026-11-06", steps = 1000, calories = 40, water = 250)
        dao.insert(record)
        dao.insert(record.copy(steps = 500, water = 750))
        dao.insert(record.copy(date = "2026-11-05", steps = 9000))
        assertEquals(DailyTotals(1500, 80, 1000), dao.observeTotals(record.date).first())
        assertEquals(DailyTotals(), dao.observeTotals("2020-01-01").first())
    }
    @Test fun dashboardSumsDoNotOverflowInt() = runBlocking {
        val dao = db.records()
        val record = HealthRecord(date = "2026-11-06", steps = Int.MAX_VALUE, calories = 1, water = 1)
        dao.insert(record)
        dao.insert(record)
        assertEquals(Int.MAX_VALUE.toLong() * 2, dao.observeTotals(record.date).first().steps)
    }
    @Test fun savedValuesSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "healthmate-test-${UUID.randomUUID()}.db"
        var persistent = Room.databaseBuilder(context, HealthDatabase::class.java, name).build()
        try {
            persistent.records().insert(HealthRecord(date = "2026-11-06", steps = 1200, calories = 60, water = 500))
            persistent.close()
            persistent = Room.databaseBuilder(context, HealthDatabase::class.java, name).build()
            assertEquals(DailyTotals(1200, 60, 500), persistent.records().observeTotals("2026-11-06").first())
        } finally { persistent.close(); context.deleteDatabase(name) }
    }
    @Test fun goalsMigrationKeepsExistingRecordsAndPersistsTargets() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "healthmate-migration-${UUID.randomUUID()}.db"
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("CREATE TABLE health_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, steps INTEGER NOT NULL, calories INTEGER NOT NULL, water INTEGER NOT NULL)")
            old.execSQL("CREATE INDEX index_health_records_date ON health_records(date)")
            old.execSQL("INSERT INTO health_records(date,steps,calories,water) VALUES ('2026-11-06',1200,60,500)")
            old.version = 1
        }
        fun open() = Room.databaseBuilder(context, HealthDatabase::class.java, name).addMigrations(HealthDatabase.MIGRATION_1_2).build()
        var migrated = open()
        try {
            assertEquals(DailyTotals(1200,60,500), migrated.records().observeTotals("2026-11-06").first())
            assertNull(migrated.records().observeGoals().first())
            val goals = DailyGoals(steps = 6000, calories = 300, water = 1500)
            migrated.records().saveGoals(goals)
            migrated.close()
            migrated = open()
            assertEquals(goals, migrated.records().observeGoals().first())
            assertEquals(DailyTotals(1200,60,500), migrated.records().observeTotals("2026-11-06").first())
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}

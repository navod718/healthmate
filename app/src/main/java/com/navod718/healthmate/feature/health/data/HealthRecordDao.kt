package com.navod718.healthmate.feature.health.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Minimal persistence for Add Record and Dashboard. Records management is a separate workstream.
@Dao
interface HealthRecordDao {
    @Query("SELECT COALESCE(SUM(steps), 0) AS steps, COALESCE(SUM(calories), 0) AS calories, COALESCE(SUM(water), 0) AS water FROM health_records WHERE date = :date")
    fun observeTotals(date: String): Flow<DailyTotals>

    @Query("SELECT * FROM daily_goals WHERE id = 1")
    fun observeGoals(): Flow<DailyGoals?>

    @androidx.room.Upsert suspend fun saveGoals(goals: DailyGoals)

    @Insert suspend fun insert(record: HealthRecord): Long
}

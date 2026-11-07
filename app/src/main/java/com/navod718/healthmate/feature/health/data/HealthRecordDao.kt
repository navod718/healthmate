package com.navod718.healthmate.feature.health.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Shared persistence for tracking, records management and daily goals.
@Dao
interface HealthRecordDao {
    @Query("SELECT COALESCE(SUM(steps), 0) AS steps, COALESCE(SUM(calories), 0) AS calories, COALESCE(SUM(water), 0) AS water FROM health_records WHERE date = :date")
    fun observeTotals(date: String): Flow<DailyTotals>

    @Query("SELECT date, SUM(steps) AS steps, SUM(calories) AS calories, SUM(water) AS water FROM health_records WHERE date >= :start AND date <= :end GROUP BY date ORDER BY date ASC")
    fun observeDailyActivity(start: String, end: String): Flow<List<DailyActivity>>

    @Query("SELECT * FROM daily_goals WHERE id = 1")
    fun observeGoals(): Flow<DailyGoals?>

    @androidx.room.Upsert suspend fun saveGoals(goals: DailyGoals)

    @Insert suspend fun insert(record: HealthRecord): Long

    @Query("SELECT * FROM health_records WHERE (:date IS NULL OR date = :date) ORDER BY date DESC, id DESC")
    fun observeRecords(date: String?): Flow<List<HealthRecord>>

    @Query("SELECT COUNT(*) FROM health_records")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM health_records")
    suspend fun count(): Int

    @Query("SELECT * FROM health_records WHERE id = :id")
    suspend fun find(id: Long): HealthRecord?

    @Update suspend fun update(record: HealthRecord): Int

    @Query("DELETE FROM health_records WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Transaction
    suspend fun loadSamplesIfEmpty(records: List<HealthRecord>): Boolean {
        if (count() != 0) return false
        records.forEach { insert(it) }
        return true
    }
}

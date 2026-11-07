package com.navod718.healthmate.feature.health.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "health_records", indices = [Index("date")])
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val steps: Int,
    val calories: Int,
    val water: Int,
)

data class DailyTotals(val steps: Long = 0, val calories: Long = 0, val water: Long = 0)

// A query result, not an additional database table.
data class DailyActivity(val date: String, val steps: Long = 0, val calories: Long = 0, val water: Long = 0)

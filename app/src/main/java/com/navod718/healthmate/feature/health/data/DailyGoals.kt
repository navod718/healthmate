package com.navod718.healthmate.feature.health.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoals(
    @PrimaryKey val id: Int = 1,
    val steps: Int = 8000,
    val calories: Int = 400,
    val water: Int = 2000,
) {
    fun isValid() = id == 1 && steps > 0 && calories > 0 && water > 0
    fun progress(value: Long, goal: Int): Float = (value.toDouble() / goal.coerceAtLeast(1)).coerceIn(0.0, 1.0).toFloat()
}

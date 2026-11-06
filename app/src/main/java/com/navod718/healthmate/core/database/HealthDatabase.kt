package com.navod718.healthmate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.navod718.healthmate.feature.health.data.HealthRecord
import com.navod718.healthmate.feature.health.data.HealthRecordDao

@Database(entities = [HealthRecord::class], version = 1, exportSchema = true)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun records(): HealthRecordDao
}

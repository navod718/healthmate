package com.navod718.healthmate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.navod718.healthmate.feature.health.data.DailyGoals
import com.navod718.healthmate.feature.health.data.HealthRecord
import com.navod718.healthmate.feature.health.data.HealthRecordDao

@Database(entities = [HealthRecord::class, DailyGoals::class], version = 2, exportSchema = true)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun records(): HealthRecordDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `daily_goals` (`id` INTEGER NOT NULL, `steps` INTEGER NOT NULL, `calories` INTEGER NOT NULL, `water` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}

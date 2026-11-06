package com.navod718.healthmate

import android.app.Application
import androidx.room.Room
import com.navod718.healthmate.core.database.HealthDatabase
import com.navod718.healthmate.feature.health.data.HealthRepository

class HealthMateApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, HealthDatabase::class.java, "healthmate.db").addMigrations(HealthDatabase.MIGRATION_1_2).build()
    }
    val repository by lazy { HealthRepository(database.records()) }
}

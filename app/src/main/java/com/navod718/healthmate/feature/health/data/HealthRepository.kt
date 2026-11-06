package com.navod718.healthmate.feature.health.data

import kotlinx.coroutines.flow.map
import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.core.validation.RecordValidator

class HealthRepository(private val dao: HealthRecordDao) {
    fun observeTotals(date: String) = dao.observeTotals(date)

    fun observeGoals() = dao.observeGoals().map { it ?: DailyGoals() }
    suspend fun saveGoals(goals: DailyGoals) {
        require(goals.isValid()) { "Goals must be positive whole numbers." }
        dao.saveGoals(goals)
    }

    suspend fun add(input: RecordInput) {
        require(RecordValidator.validate(input).isEmpty()) { "Check the record values." }
        dao.insert(HealthRecord(date = input.date, steps = input.steps.toInt(), calories = input.calories.toInt(), water = input.water.toInt()))
    }
}

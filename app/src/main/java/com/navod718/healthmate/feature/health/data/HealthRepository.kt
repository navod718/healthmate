package com.navod718.healthmate.feature.health.data

import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.core.validation.RecordValidator

class HealthRepository(private val dao: HealthRecordDao) {
    fun observeTotals(date: String) = dao.observeTotals(date)

    suspend fun add(input: RecordInput) {
        require(RecordValidator.validate(input).isEmpty()) { "Check the record values." }
        dao.insert(HealthRecord(date = input.date, steps = input.steps.toInt(), calories = input.calories.toInt(), water = input.water.toInt()))
    }
}

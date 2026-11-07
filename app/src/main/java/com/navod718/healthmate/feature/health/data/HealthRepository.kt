package com.navod718.healthmate.feature.health.data

import java.time.LocalDate
import kotlinx.coroutines.flow.map
import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.core.validation.RecordValidator

class HealthRepository(private val dao: HealthRecordDao) {
    fun observeTotals(date: String) = dao.observeTotals(date)

    fun observeWeek(end: String): kotlinx.coroutines.flow.Flow<List<DailyActivity>> {
        val last = LocalDate.parse(end)
        val first = last.minusDays(6)
        return dao.observeDailyActivity(first.toString(), end).map { rows ->
            val byDate = rows.associateBy { it.date }
            (0L..6L).map { offset ->
                val date = first.plusDays(offset).toString()
                byDate[date] ?: DailyActivity(date)
            }
        }
    }
    suspend fun addWater(amount: Int) {
        require(amount == 250 || amount == 500) { "Choose 250 ml or 500 ml." }
        add(RecordInput(date = LocalDate.now().toString(), steps = "0", calories = "0", water = amount.toString()))
    }

    fun observeGoals() = dao.observeGoals().map { it ?: DailyGoals() }
    suspend fun saveGoals(goals: DailyGoals) {
        require(goals.isValid()) { "Goals must be positive whole numbers." }
        dao.saveGoals(goals)
    }

    fun observeRecords(date: String?) = dao.observeRecords(date)
    fun observeCount() = dao.observeCount()
    suspend fun find(id: Long) = dao.find(id)
    suspend fun update(id: Long, input: RecordInput): Boolean {
        require(id > 0)
        require(RecordValidator.validate(input).isEmpty()) { "Check the record values." }
        return dao.update(HealthRecord(id, input.date, input.steps.toInt(), input.calories.toInt(), input.water.toInt())) == 1
    }
    suspend fun delete(id: Long): Boolean = dao.delete(id) == 1

    suspend fun loadSamples(): Boolean {
        val today = LocalDate.now()
        return dao.loadSamplesIfEmpty(listOf(
            HealthRecord(date = today.toString(), steps = 1500, calories = 75, water = 500),
            HealthRecord(date = today.minusDays(1).toString(), steps = 6400, calories = 310, water = 1800),
            HealthRecord(date = today.minusDays(2).toString(), steps = 8200, calories = 430, water = 2200),
            HealthRecord(date = today.minusDays(3).toString(), steps = 5100, calories = 270, water = 1500),
        ))
    }

    suspend fun add(input: RecordInput) {
        require(RecordValidator.validate(input).isEmpty()) { "Check the record values." }
        dao.insert(HealthRecord(date = input.date, steps = input.steps.toInt(), calories = input.calories.toInt(), water = input.water.toInt()))
    }
}

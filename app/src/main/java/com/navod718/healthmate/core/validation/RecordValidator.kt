package com.navod718.healthmate.core.validation

import java.time.LocalDate

data class RecordInput(
    val date: String = LocalDate.now().toString(),
    val steps: String = "",
    val calories: String = "",
    val water: String = "",
)

object RecordValidator {
    internal const val EMPTY_ACTIVITY_ERROR = "Enter at least one value greater than zero."

    fun validate(input: RecordInput, today: LocalDate = LocalDate.now()): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val date = runCatching { LocalDate.parse(input.date) }.getOrNull()
        if (date == null || date.toString() != input.date) errors["date"] = "Choose a valid date."
        else if (date.isAfter(today)) errors["date"] = "Future dates are not allowed."
        mapOf("steps" to input.steps, "calories" to input.calories, "water" to input.water).forEach { (key, raw) ->
            if (raw.isBlank()) errors[key] = "This field is required."
            else if (!raw.matches(Regex("[0-9]+")) || raw.toIntOrNull() == null)
                errors[key] = "Enter a whole number from 0 to 2,147,483,647."
        }
        if (errors.isEmpty() && listOf(input.steps, input.calories, input.water).all { it.toInt() == 0 }) {
            errors["form"] = EMPTY_ACTIVITY_ERROR
        }
        return errors
    }
}

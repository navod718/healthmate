package com.navod718.healthmate

import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.core.validation.RecordValidator
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RecordValidatorTest {
    private val today = LocalDate.of(2026, 11, 6)
    private fun validate(input: RecordInput) = RecordValidator.validate(input, today)
    private val valid = RecordInput("2026-11-06", "1000", "50", "250")
    @Test fun validEntryAndPartialActivityAreAccepted() {
        assertTrue(validate(valid).isEmpty())
        assertTrue(validate(valid.copy(steps = "0", calories = "0")).isEmpty())
    }
    @Test fun missingFieldsAreRejected() {
        assertEquals(setOf("steps", "calories", "water"), validate(valid.copy(steps = "", calories = "", water = "")).keys)
    }
    @Test fun negativeDecimalTextAndOverflowAreRejected() {
        for (invalid in listOf("-1", "1.5", "walk", "2147483648", " 20", "+1")) {
            assertTrue("Expected error for $invalid", validate(valid.copy(steps = invalid)).containsKey("steps"))
        }
    }
    @Test fun zeroOnlyEntryIsRejected() {
        assertTrue(validate(valid.copy(steps = "0", calories = "0", water = "0")).containsKey("form"))
    }
    @Test fun futureAndMalformedDatesAreRejected() {
        for (invalid in listOf("2026-11-07", "2026-02-30", "06/11/2026", "")) {
            assertTrue(validate(valid.copy(date = invalid)).containsKey("date"))
        }
    }
    @Test fun pastAndLeapDayDatesAreAccepted() {
        assertTrue(validate(valid.copy(date = "2024-02-29")).isEmpty())
    }
}

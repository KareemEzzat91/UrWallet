package com.example.urwallet.features.more.domain.calculator

import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NextOccurrenceCalculatorTest {

    @Test
    fun `calculateNextOccurrence with DAILY adds exactly one day`() {
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = startCal.timeInMillis

        val next = NextOccurrenceCalculator.calculateNextOccurrence(current, Frequency.DAILY, current)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, nextCal.get(Calendar.MONTH))
        assertEquals(15, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, nextCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `calculateNextOccurrence with WEEKLY adds exactly 7 days`() {
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 9, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = startCal.timeInMillis

        val next = NextOccurrenceCalculator.calculateNextOccurrence(current, Frequency.WEEKLY, current)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, nextCal.get(Calendar.MONTH))
        assertEquals(21, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextOccurrence with MONTHLY on day 31 clamps to Feb 28 in non-leap year`() {
        val startCal = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 31, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val originalStart = startCal.timeInMillis
        val current = originalStart

        val next = NextOccurrenceCalculator.calculateNextOccurrence(current, Frequency.MONTHLY, originalStart)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2025, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH))
        assertEquals(28, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextOccurrence with MONTHLY on day 31 clamps to Feb 29 in leap year 2024`() {
        val startCal = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 31, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val originalStart = startCal.timeInMillis
        val current = originalStart

        val next = NextOccurrenceCalculator.calculateNextOccurrence(current, Frequency.MONTHLY, originalStart)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2024, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH))
        assertEquals(29, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextOccurrence with MONTHLY restores anchor day 31 when moving from Feb 28 to March`() {
        val startCal = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 31, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val originalStart = startCal.timeInMillis

        val febCal = Calendar.getInstance().apply {
            set(2025, Calendar.FEBRUARY, 28, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentInFeb = febCal.timeInMillis

        val next = NextOccurrenceCalculator.calculateNextOccurrence(currentInFeb, Frequency.MONTHLY, originalStart)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2025, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, nextCal.get(Calendar.MONTH))
        assertEquals(31, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextOccurrence with MONTHLY rolls over year from December to January`() {
        val startCal = Calendar.getInstance().apply {
            set(2025, Calendar.DECEMBER, 15, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = startCal.timeInMillis

        val next = NextOccurrenceCalculator.calculateNextOccurrence(current, Frequency.MONTHLY, current)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCal.get(Calendar.MONTH))
        assertEquals(15, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextOccurrence with YEARLY on Feb 29 leap year advances to Feb 28 in non-leap year`() {
        val leapCal = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 29, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val originalStart = leapCal.timeInMillis

        val next = NextOccurrenceCalculator.calculateNextOccurrence(originalStart, Frequency.YEARLY, originalStart)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2025, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH))
        assertEquals(28, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateMonthlyObligations normalizes expense frequencies correctly`() {
        val list = listOf(
            RecurringTransaction(
                id = 1,
                title = "Coffee",
                amount = 10.0,
                type = TransactionType.EXPENSE,
                categoryId = 1,
                frequency = Frequency.DAILY,
                startDate = 0L,
                nextOccurrence = 0L,
                isActive = true
            ), // 10 * 30 = 300
            RecurringTransaction(
                id = 2,
                title = "Internet",
                amount = 350.0,
                type = TransactionType.EXPENSE,
                categoryId = 2,
                frequency = Frequency.MONTHLY,
                startDate = 0L,
                nextOccurrence = 0L,
                isActive = true
            ), // 350
            RecurringTransaction(
                id = 3,
                title = "Insurance",
                amount = 1200.0,
                type = TransactionType.EXPENSE,
                categoryId = 3,
                frequency = Frequency.YEARLY,
                startDate = 0L,
                nextOccurrence = 0L,
                isActive = true
            ), // 1200 / 12 = 100
            RecurringTransaction(
                id = 4,
                title = "Paused Expense",
                amount = 500.0,
                type = TransactionType.EXPENSE,
                categoryId = 4,
                frequency = Frequency.MONTHLY,
                startDate = 0L,
                nextOccurrence = 0L,
                isActive = false // inactive, should not count
            ),
            RecurringTransaction(
                id = 5,
                title = "Salary",
                amount = 10000.0,
                type = TransactionType.INCOME,
                categoryId = 5,
                frequency = Frequency.MONTHLY,
                startDate = 0L,
                nextOccurrence = 0L,
                isActive = true // income, should not count in obligations
            )
        )

        val obligations = NextOccurrenceCalculator.calculateMonthlyObligations(list)
        // Expected: 300 + 350 + 100 = 750.0
        assertEquals(750.0, obligations, 0.001)

        val income = NextOccurrenceCalculator.calculateMonthlyRecurringIncome(list)
        assertEquals(10000.0, income, 0.001)
    }
}

package com.example.urwallet.features.more.domain.calculator

import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import java.util.Calendar

object NextOccurrenceCalculator {

    /**
     * Calculates the deterministic next occurrence date for a recurring transaction.
     *
     * @param currentOccurrence The epoch timestamp of the current occurrence.
     * @param frequency The recurrence frequency (DAILY, WEEKLY, MONTHLY, YEARLY).
     * @param originalStartDate The epoch timestamp of the original start date, used as an anchor
     *                           to preserve the scheduled day-of-month across varying month lengths.
     * @return The epoch timestamp of the next occurrence.
     */
    fun calculateNextOccurrence(
        currentOccurrence: Long,
        frequency: Frequency,
        originalStartDate: Long
    ): Long {
        val currentCal = Calendar.getInstance().apply { timeInMillis = currentOccurrence }
        val startCal = Calendar.getInstance().apply { timeInMillis = originalStartDate }

        val anchorDayOfMonth = startCal.get(Calendar.DAY_OF_MONTH)
        val hourOfDay = currentCal.get(Calendar.HOUR_OF_DAY)
        val minute = currentCal.get(Calendar.MINUTE)
        val second = currentCal.get(Calendar.SECOND)
        val millisecond = currentCal.get(Calendar.MILLISECOND)

        return when (frequency) {
            Frequency.DAILY -> {
                currentCal.add(Calendar.DAY_OF_MONTH, 1)
                currentCal.timeInMillis
            }

            Frequency.WEEKLY -> {
                currentCal.add(Calendar.DAY_OF_MONTH, 7)
                currentCal.timeInMillis
            }

            Frequency.MONTHLY -> {
                val nextCal = Calendar.getInstance().apply {
                    timeInMillis = currentOccurrence
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, second)
                    set(Calendar.MILLISECOND, millisecond)
                }

                // Advance by 1 month, setting day to 1 first to avoid month overflow
                var targetYear = nextCal.get(Calendar.YEAR)
                var targetMonth = nextCal.get(Calendar.MONTH) + 1
                if (targetMonth > Calendar.DECEMBER) {
                    targetMonth = Calendar.JANUARY
                    targetYear += 1
                }

                nextCal.set(Calendar.YEAR, targetYear)
                nextCal.set(Calendar.MONTH, targetMonth)
                nextCal.set(Calendar.DAY_OF_MONTH, 1)

                val maxDaysInTargetMonth = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val targetDay = minOf(anchorDayOfMonth, maxDaysInTargetMonth)
                nextCal.set(Calendar.DAY_OF_MONTH, targetDay)

                nextCal.timeInMillis
            }

            Frequency.YEARLY -> {
                val nextCal = Calendar.getInstance().apply {
                    timeInMillis = currentOccurrence
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, second)
                    set(Calendar.MILLISECOND, millisecond)
                }

                val targetYear = nextCal.get(Calendar.YEAR) + 1
                val targetMonth = startCal.get(Calendar.MONTH)

                nextCal.set(Calendar.YEAR, targetYear)
                nextCal.set(Calendar.MONTH, targetMonth)
                nextCal.set(Calendar.DAY_OF_MONTH, 1)

                val maxDaysInTargetMonth = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val targetDay = minOf(anchorDayOfMonth, maxDaysInTargetMonth)
                nextCal.set(Calendar.DAY_OF_MONTH, targetDay)

                nextCal.timeInMillis
            }
        }
    }

    /**
     * Calculates the total monthly recurring expense obligations.
     * Standardizes all active recurring expense amounts to their monthly equivalent.
     */
    fun calculateMonthlyObligations(recurringList: List<RecurringTransaction>): Double {
        return recurringList
            .filter { it.isActive && it.type == TransactionType.EXPENSE }
            .sumOf { toMonthlyAmount(it.amount, it.frequency) }
    }

    /**
     * Calculates the total monthly recurring income.
     * Standardizes all active recurring income amounts to their monthly equivalent.
     */
    fun calculateMonthlyRecurringIncome(recurringList: List<RecurringTransaction>): Double {
        return recurringList
            .filter { it.isActive && it.type == TransactionType.INCOME }
            .sumOf { toMonthlyAmount(it.amount, it.frequency) }
    }

    private fun toMonthlyAmount(amount: Double, frequency: Frequency): Double {
        return when (frequency) {
            Frequency.DAILY -> amount * 30.0
            Frequency.WEEKLY -> amount * (52.0 / 12.0)
            Frequency.MONTHLY -> amount
            Frequency.YEARLY -> amount / 12.0
        }
    }
}

package com.example.urwallet.features.challenges.domain.calculator

import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.challenges.domain.model.Challenge
import com.example.urwallet.features.challenges.domain.model.ChallengeDayStatus
import com.example.urwallet.features.challenges.domain.model.ChallengeProgress
import com.example.urwallet.features.challenges.domain.model.DayStatus
import com.example.urwallet.features.transactions.domain.model.Transaction
import java.util.Calendar

object ChallengeEvaluator {

    fun evaluate(
        challenge: Challenge,
        transactions: List<Transaction>,
        savedAmount: Double = 0.0,
        currentTimeMillis: Long = DateUtils.getCurrentEpochMs()
    ): ChallengeProgress {
        return when (challenge.type) {
            ChallengeType.NO_SPENDING -> evaluateNoSpending(challenge, transactions, currentTimeMillis)
            ChallengeType.SAVE_AMOUNT -> evaluateSaveAmount(challenge, savedAmount, currentTimeMillis)
            ChallengeType.REDUCE_CATEGORY -> evaluateReduceCategory(challenge, transactions, currentTimeMillis)
        }
    }

    private fun evaluateNoSpending(
        challenge: Challenge,
        transactions: List<Transaction>,
        currentTimeMillis: Long
    ): ChallengeProgress {
        val startDayMs = DateUtils.getStartOfDay(challenge.startDate)
        val endDayMs = DateUtils.getEndOfDay(challenge.endDate)
        val todayStartMs = DateUtils.getStartOfDay(currentTimeMillis)

        val calendar = Calendar.getInstance().apply { timeInMillis = startDayMs }
        val dayTimestamps = mutableListOf<Long>()
        while (calendar.timeInMillis <= endDayMs) {
            dayTimestamps.add(calendar.timeInMillis)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Filter transactions within challenge window
        val relevantTransactions = transactions.filter {
            it.type == TransactionType.EXPENSE &&
                    it.date in startDayMs..endDayMs &&
                    (challenge.categoryId == null || it.categoryId == challenge.categoryId)
        }

        val dailyStatuses = mutableListOf<ChallengeDayStatus>()
        var dayNumber = 1
        var tempStreak = 0
        var longestStreak = 0
        var completedDays = 0

        for (dayStart in dayTimestamps) {
            val dayEnd = DateUtils.getEndOfDay(dayStart)
            val dayExpenses = relevantTransactions.filter { it.date in dayStart..dayEnd }
            val daySpent = dayExpenses.sumOf { it.amount }

            val status = when {
                dayStart > todayStartMs -> DayStatus.PENDING
                dayStart == todayStartMs -> {
                    if (daySpent > 0.0) DayStatus.FAILED else DayStatus.TODAY
                }
                else -> { // Past day
                    if (daySpent > 0.0) DayStatus.FAILED else DayStatus.COMPLETED
                }
            }

            if (status == DayStatus.COMPLETED) {
                completedDays++
            }

            // Streak evaluation for past days and today
            if (dayStart <= todayStartMs) {
                if (status == DayStatus.COMPLETED || status == DayStatus.TODAY) {
                    tempStreak++
                    if (tempStreak > longestStreak) {
                        longestStreak = tempStreak
                    }
                } else if (status == DayStatus.FAILED) {
                    tempStreak = 0
                }
            }

            dailyStatuses.add(
                ChallengeDayStatus(
                    dayNumber = dayNumber++,
                    dateMillis = dayStart,
                    status = status,
                    spentAmount = daySpent
                )
            )
        }

        val currentStreak = tempStreak
        val targetDays = challenge.targetDays ?: dayTimestamps.size
        val effectiveProgress = maxOf(completedDays, longestStreak).toDouble()
        val progressPercentage = if (targetDays > 0) {
            ((effectiveProgress / targetDays) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val remainingDays = if (endDayMs >= todayStartMs) {
            val diff = ((endDayMs - todayStartMs) / (24 * 60 * 60 * 1000)).toInt()
            maxOf(0, diff)
        } else {
            0
        }

        val isCompleted = challenge.isCompleted || (targetDays > 0 && (completedDays >= targetDays || longestStreak >= targetDays))

        return ChallengeProgress(
            challenge = challenge,
            currentProgress = effectiveProgress,
            targetAmount = challenge.targetAmount,
            targetDays = targetDays,
            completedDays = completedDays,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            remainingDays = remainingDays,
            progressPercentage = progressPercentage,
            isCompleted = isCompleted,
            dailyStatuses = dailyStatuses
        )
    }

    private fun evaluateSaveAmount(
        challenge: Challenge,
        savedAmount: Double,
        currentTimeMillis: Long
    ): ChallengeProgress {
        val targetAmount = challenge.targetAmount ?: 0.0
        val safeSaved = maxOf(0.0, savedAmount)
        val progressPercentage = if (targetAmount > 0.0) {
            ((safeSaved / targetAmount) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val todayStartMs = DateUtils.getStartOfDay(currentTimeMillis)
        val endDayMs = DateUtils.getEndOfDay(challenge.endDate)
        val remainingDays = if (endDayMs >= todayStartMs) {
            val diff = ((endDayMs - todayStartMs) / (24 * 60 * 60 * 1000)).toInt()
            maxOf(0, diff)
        } else {
            0
        }

        val isCompleted = challenge.isCompleted || (targetAmount > 0.0 && safeSaved >= targetAmount)

        return ChallengeProgress(
            challenge = challenge,
            currentProgress = safeSaved,
            targetAmount = targetAmount,
            targetDays = challenge.targetDays,
            completedDays = 0,
            currentStreak = 0,
            longestStreak = 0,
            remainingDays = remainingDays,
            progressPercentage = progressPercentage,
            isCompleted = isCompleted,
            dailyStatuses = emptyList()
        )
    }

    private fun evaluateReduceCategory(
        challenge: Challenge,
        transactions: List<Transaction>,
        currentTimeMillis: Long
    ): ChallengeProgress {
        val startDayMs = DateUtils.getStartOfDay(challenge.startDate)
        val endDayMs = DateUtils.getEndOfDay(challenge.endDate)
        val todayStartMs = DateUtils.getStartOfDay(currentTimeMillis)

        val relevantTransactions = transactions.filter {
            it.type == TransactionType.EXPENSE &&
                    it.date in startDayMs..endDayMs &&
                    (challenge.categoryId == null || it.categoryId == challenge.categoryId)
        }

        val totalSpent = relevantTransactions.sumOf { it.amount }
        val limitAmount = challenge.targetAmount ?: 0.0

        val progressPercentage = if (limitAmount > 0.0) {
            ((totalSpent / limitAmount) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val remainingDays = if (endDayMs >= todayStartMs) {
            val diff = ((endDayMs - todayStartMs) / (24 * 60 * 60 * 1000)).toInt()
            maxOf(0, diff)
        } else {
            0
        }

        // Completed if period has finished and spending stayed within limit
        val isCompleted = challenge.isCompleted || (currentTimeMillis >= endDayMs && limitAmount > 0.0 && totalSpent <= limitAmount)

        return ChallengeProgress(
            challenge = challenge,
            currentProgress = totalSpent,
            targetAmount = limitAmount,
            targetDays = challenge.targetDays,
            completedDays = 0,
            currentStreak = 0,
            longestStreak = 0,
            remainingDays = remainingDays,
            progressPercentage = progressPercentage,
            isCompleted = isCompleted,
            dailyStatuses = emptyList()
        )
    }
}

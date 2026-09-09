package com.example.urwallet.features.challenges.domain.calculator

import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.challenges.domain.model.Challenge
import com.example.urwallet.features.challenges.domain.model.DayStatus
import com.example.urwallet.features.transactions.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ChallengeEvaluatorTest {

    private val oneDayMs = 24 * 60 * 60 * 1000L

    @Test
    fun `NO_SPENDING with all days zero expenses yields full streak and completion`() {
        val start = DateUtils.getStartOfDay() - (4 * oneDayMs)
        val end = start + (4 * oneDayMs) + (oneDayMs - 1) // 5 days total
        val today = DateUtils.getStartOfDay()

        val challenge = Challenge(
            id = 1L,
            title = "No Spend 5 Days",
            description = "Test",
            type = ChallengeType.NO_SPENDING,
            targetDays = 5,
            startDate = start,
            endDate = end
        )

        val progress = ChallengeEvaluator.evaluate(
            challenge = challenge,
            transactions = emptyList(),
            currentTimeMillis = today
        )

        assertEquals(4, progress.completedDays) // 4 past days completed
        assertEquals(5, progress.currentStreak) // 4 past + today
        assertEquals(5, progress.longestStreak)
        assertEquals(100.0, progress.progressPercentage, 0.01)
        assertTrue(progress.isCompleted)
    }

    @Test
    fun `NO_SPENDING streak semantics with failure on day 3`() {
        // 4 past days (Days 1, 2, 3, 4) + today (Day 5)
        val day1 = DateUtils.getStartOfDay() - (4 * oneDayMs)
        val day2 = DateUtils.getStartOfDay() - (3 * oneDayMs)
        val day3 = DateUtils.getStartOfDay() - (2 * oneDayMs)
        val day4 = DateUtils.getStartOfDay() - (1 * oneDayMs)
        val today = DateUtils.getStartOfDay()
        val end = today + (oneDayMs - 1)

        val challenge = Challenge(
            id = 1L,
            title = "Streak test",
            description = "Test",
            type = ChallengeType.NO_SPENDING,
            targetDays = 5,
            startDate = day1,
            endDate = end
        )

        // Expense on Day 3 only
        val transactions = listOf(
            Transaction(
                id = 101L,
                title = "Coffee",
                amount = 50.0,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                date = day3 + 1000L
            )
        )

        val progress = ChallengeEvaluator.evaluate(
            challenge = challenge,
            transactions = transactions,
            currentTimeMillis = today
        )

        // Day 1 ✅, Day 2 ✅, Day 3 ❌, Day 4 ✅, Day 5 (Today) ✅
        assertEquals(3, progress.completedDays) // Days 1, 2, 4 are past COMPLETED
        assertEquals(2, progress.currentStreak) // Day 4 + Day 5 (Today)
        assertEquals(2, progress.longestStreak) // Max of (Day1-2: 2) and (Day4-5: 2)

        val day3Status = progress.dailyStatuses.find { it.dateMillis == day3 }
        assertEquals(DayStatus.FAILED, day3Status?.status)
    }

    @Test
    fun `category-specific NO_SPENDING ignores expenses in other categories`() {
        val start = DateUtils.getStartOfDay() - (2 * oneDayMs)
        val end = start + (2 * oneDayMs) + (oneDayMs - 1)
        val today = DateUtils.getStartOfDay()

        val challenge = Challenge(
            id = 2L,
            title = "No Cafe",
            description = "Cafe category only",
            type = ChallengeType.NO_SPENDING,
            targetDays = 3,
            categoryId = 10L, // Cafe category
            startDate = start,
            endDate = end
        )

        // Transaction in Groceries (categoryId = 20L)
        val transactions = listOf(
            Transaction(
                id = 1L,
                title = "Groceries",
                amount = 300.0,
                type = TransactionType.EXPENSE,
                categoryId = 20L, // Not 10L!
                date = start + 5000L
            )
        )

        val progress = ChallengeEvaluator.evaluate(
            challenge = challenge,
            transactions = transactions,
            currentTimeMillis = today
        )

        // The day with groceries expense should NOT fail because categoryId is different!
        assertEquals(2, progress.completedDays)
        assertEquals(3, progress.currentStreak)
        assertTrue(progress.isCompleted)
    }

    @Test
    fun `SAVE_AMOUNT evaluates target progress and completes when reached`() {
        val start = DateUtils.getStartOfDay()
        val end = start + (30 * oneDayMs)

        val challenge = Challenge(
            id = 3L,
            title = "Save 1000",
            description = "Test",
            type = ChallengeType.SAVE_AMOUNT,
            targetAmount = 1000.0,
            startDate = start,
            endDate = end
        )

        val partialProgress = ChallengeEvaluator.evaluate(
            challenge = challenge,
            transactions = emptyList(),
            savedAmount = 400.0
        )
        assertEquals(40.0, partialProgress.progressPercentage, 0.01)
        assertFalse(partialProgress.isCompleted)

        val completedProgress = ChallengeEvaluator.evaluate(
            challenge = challenge,
            transactions = emptyList(),
            savedAmount = 1200.0
        )
        assertEquals(100.0, completedProgress.progressPercentage, 0.01) // clamped
        assertTrue(completedProgress.isCompleted)
    }

    @Test
    fun `REDUCE_CATEGORY monitors spending limit and completes after period if within ceiling`() {
        val start = DateUtils.getStartOfDay() - (10 * oneDayMs)
        val end = start + (10 * oneDayMs) - 1 // Finished yesterday!
        val today = DateUtils.getStartOfDay()

        val challenge = Challenge(
            id = 4L,
            title = "Shopping limit 500",
            description = "Test",
            type = ChallengeType.REDUCE_CATEGORY,
            targetAmount = 500.0,
            categoryId = 5L,
            startDate = start,
            endDate = end
        )

        val transactionsUnderLimit = listOf(
            Transaction(
                id = 1L,
                title = "Shirt",
                amount = 300.0,
                type = TransactionType.EXPENSE,
                categoryId = 5L,
                date = start + (2 * oneDayMs)
            )
        )

        val progress = ChallengeEvaluator.evaluate(
            challenge = challenge,
            transactions = transactionsUnderLimit,
            currentTimeMillis = today
        )

        assertEquals(300.0, progress.currentProgress, 0.01)
        assertEquals(60.0, progress.progressPercentage, 0.01)
        assertTrue("Challenge should be completed as period ended under budget", progress.isCompleted)
    }

    @Test
    fun `REDUCE_CATEGORY marks progress over 100 percent when limit exceeded`() {
        val start = DateUtils.getStartOfDay()
        val end = start + (10 * oneDayMs)

        val challenge = Challenge(
            id = 5L,
            title = "Shopping limit 500",
            description = "Test",
            type = ChallengeType.REDUCE_CATEGORY,
            targetAmount = 500.0,
            categoryId = 5L,
            startDate = start,
            endDate = end
        )

        val transactionsOverLimit = listOf(
            Transaction(
                id = 1L,
                title = "Shopping spree",
                amount = 700.0,
                type = TransactionType.EXPENSE,
                categoryId = 5L,
                date = start + 1000L
            )
        )

        val progress = ChallengeEvaluator.evaluate(
            challenge = challenge,
            transactions = transactionsOverLimit,
            currentTimeMillis = start
        )

        assertEquals(700.0, progress.currentProgress, 0.01)
        assertEquals(100.0, progress.progressPercentage, 0.01)
        assertFalse(progress.isCompleted)
    }

    @Test
    fun `zero target amounts or negative values are handled safely without crash`() {
        val challenge = Challenge(
            id = 6L,
            title = "Zero Target",
            description = "Edge case",
            type = ChallengeType.SAVE_AMOUNT,
            targetAmount = 0.0,
            startDate = 1000L,
            endDate = 2000L
        )

        val progress = ChallengeEvaluator.evaluate(
            challenge = challenge,
            transactions = emptyList(),
            savedAmount = 0.0
        )

        assertEquals(0.0, progress.progressPercentage, 0.01)
    }
}

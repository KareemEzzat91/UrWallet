package com.example.urwallet.features.goals.domain.calculator

import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.features.goals.domain.model.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalCalculatorTest {

    @Test
    fun `when goal has zero contributions, savedAmount is 0 and progress is 0`() {
        val saved = GoalCalculator.calculateSavedAmount(null)
        assertEquals(0.0, saved, 0.001)

        val progress = GoalCalculator.calculateProgress(targetAmount = 5000.0, savedAmount = saved)
        assertEquals(0.0, progress, 0.001)

        val progressPct = GoalCalculator.calculateProgressPercentage(targetAmount = 5000.0, savedAmount = saved)
        assertEquals(0.0, progressPct, 0.001)

        val remaining = GoalCalculator.calculateRemainingAmount(targetAmount = 5000.0, savedAmount = saved)
        assertEquals(5000.0, remaining, 0.001)

        val completed = GoalCalculator.isGoalCompleted(targetAmount = 5000.0, savedAmount = saved)
        assertFalse(completed)
    }

    @Test
    fun `when goal has partial contributions, progress and remaining are accurate`() {
        val target = 10000.0
        val saved = 2500.0

        assertEquals(7500.0, GoalCalculator.calculateRemainingAmount(target, saved), 0.001)
        assertEquals(0.25, GoalCalculator.calculateProgress(target, saved), 0.001)
        assertEquals(25.0, GoalCalculator.calculateProgressPercentage(target, saved), 0.001)
        assertFalse(GoalCalculator.isGoalCompleted(target, saved))
    }

    @Test
    fun `when savedAmount equals or exceeds targetAmount, goal is completed`() {
        val target = 5000.0

        // Exact match
        assertTrue(GoalCalculator.isGoalCompleted(target, 5000.0))
        assertEquals(0.0, GoalCalculator.calculateRemainingAmount(target, 5000.0), 0.001)
        assertEquals(100.0, GoalCalculator.calculateProgressPercentage(target, 5000.0), 0.001)

        // Exceeded
        assertTrue(GoalCalculator.isGoalCompleted(target, 6500.0))
        assertEquals(0.0, GoalCalculator.calculateRemainingAmount(target, 6500.0), 0.001)
        assertEquals(100.0, GoalCalculator.calculateProgressPercentage(target, 6500.0), 0.001)
    }

    @Test
    fun `when targetAmount is zero or negative, calculations return safe defaults without crash`() {
        assertEquals(0.0, GoalCalculator.calculateProgress(0.0, 500.0), 0.001)
        assertEquals(0.0, GoalCalculator.calculateProgressPercentage(0.0, 500.0), 0.001)
        assertEquals(0.0, GoalCalculator.calculateRemainingAmount(0.0, 500.0), 0.001)
        assertFalse(GoalCalculator.isGoalCompleted(0.0, 500.0))
    }

    @Test
    fun `Goal domain model correctly derives isCompleted and remainingAmount`() {
        val goal = Goal(
            name = "سيارة جديدة",
            icon = "🚗",
            targetAmount = 50000.0,
            savedAmount = 0.0,
            paceMode = GoalPaceMode.BALANCED,
            monthlyTarget = 4166.66,
            deadline = System.currentTimeMillis() + 100000L
        )

        assertEquals(50000.0, goal.remainingAmount, 0.001)
        assertEquals(0.0, goal.progressPercentage, 0.001)
        assertFalse(goal.isCompleted)

        val fundedGoal = goal.copy(savedAmount = 50000.0)
        assertEquals(0.0, fundedGoal.remainingAmount, 0.001)
        assertEquals(100.0, fundedGoal.progressPercentage, 0.001)
        assertTrue(fundedGoal.isCompleted)
    }

    @Test
    fun `calculateDaysRemaining returns correct remaining days`() {
        val now = 1000000000000L
        val oneDayLater = now + (24 * 60 * 60 * 1000L)
        val tenDaysLater = now + (10 * 24 * 60 * 60 * 1000L)

        assertEquals(1L, GoalCalculator.calculateDaysRemaining(oneDayLater, now))
        assertEquals(10L, GoalCalculator.calculateDaysRemaining(tenDaysLater, now))
        assertEquals(0L, GoalCalculator.calculateDaysRemaining(now - 5000L, now))
    }

    @Test
    fun `calculateRequiredMonthlySavings returns correct monthly required amount`() {
        val now = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.JANUARY, 1, 0, 0, 0)
        }.timeInMillis

        val sixMonthsLater = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.JULY, 1, 0, 0, 0)
        }.timeInMillis

        // Target = 6,000, Saved = 0, Months = 6 -> Required = 1,000
        val required = GoalCalculator.calculateRequiredMonthlySavings(
            targetAmount = 6000.0,
            savedAmount = 0.0,
            deadline = sixMonthsLater,
            currentTimeMillis = now
        )
        assertEquals(1000.0, required, 0.001)

        // Target = 6,000, Saved = 3,000, Months = 6 -> Required = 500
        val requiredPartial = GoalCalculator.calculateRequiredMonthlySavings(
            targetAmount = 6000.0,
            savedAmount = 3000.0,
            deadline = sixMonthsLater,
            currentTimeMillis = now
        )
        assertEquals(500.0, requiredPartial, 0.001)

        // When saved equals or exceeds target -> Required = 0.0
        val requiredCompleted = GoalCalculator.calculateRequiredMonthlySavings(
            targetAmount = 6000.0,
            savedAmount = 6000.0,
            deadline = sixMonthsLater,
            currentTimeMillis = now
        )
        assertEquals(0.0, requiredCompleted, 0.001)
    }
}

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
}

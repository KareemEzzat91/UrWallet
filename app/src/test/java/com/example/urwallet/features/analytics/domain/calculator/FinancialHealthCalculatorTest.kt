package com.example.urwallet.features.analytics.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialHealthCalculatorTest {

    @Test
    fun `when user has no active goals, savingScore is 0 and does not crash`() {
        val score = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = true,
            hasActiveGoals = false,
            hasActiveRecurringTransactions = true,
            goalContributionsThisMonth = 0.0,
            activeGoalsMonthlyTarget = 0.0,
            globalBudgetAmount = 5000.0,
            totalExpensesThisMonth = 2000.0
        )

        assertEquals(0.0, score.savingScore, 0.001)
        // Planning: 40 (budget) + 0 (no goals) + 30 (recurring) = 70
        assertEquals(70.0, score.planningScore, 0.001)
        // Control: expenses 2000 / 5000 = 40% <= 50% -> full 100
        assertEquals(100.0, score.controlScore, 0.001)
        // Overall: (70 + 0 + 100) / 3 = 56.66 -> 57
        assertEquals(57, score.overallScore)
    }

    @Test
    fun `when monthlyTarget is zero or negative, savingScore falls back to 0 without division by zero`() {
        val score = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = false,
            hasActiveGoals = true,
            hasActiveRecurringTransactions = false,
            goalContributionsThisMonth = 500.0,
            activeGoalsMonthlyTarget = 0.0,
            globalBudgetAmount = 0.0,
            totalExpensesThisMonth = 0.0
        )

        assertEquals(0.0, score.savingScore, 0.001)
        assertFalse(score.savingScore.isNaN())
        assertFalse(score.savingScore.isInfinite())
    }

    @Test
    fun `when no global budget is set, controlScore defaults to neutral 50`() {
        val score = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = false,
            hasActiveGoals = false,
            hasActiveRecurringTransactions = false,
            goalContributionsThisMonth = 0.0,
            activeGoalsMonthlyTarget = 0.0,
            globalBudgetAmount = 0.0,
            totalExpensesThisMonth = 1500.0
        )

        assertEquals(50.0, score.controlScore, 0.001)
        assertEquals(0.0, score.planningScore, 0.001)
        assertEquals(0.0, score.savingScore, 0.001)
        assertEquals(17, score.overallScore) // (0 + 0 + 50) / 3 = 16.66 -> 17
    }

    @Test
    fun `when budget is exceeded, controlScore is penalized down to 0`() {
        val score = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = true,
            hasActiveGoals = false,
            hasActiveRecurringTransactions = false,
            goalContributionsThisMonth = 0.0,
            activeGoalsMonthlyTarget = 0.0,
            globalBudgetAmount = 2000.0,
            totalExpensesThisMonth = 3000.0 // 150% spent
        )

        // 100 - (150 - 50) * 2 = 100 - 200 = -100 -> clamped to 0.0
        assertEquals(0.0, score.controlScore, 0.001)
    }

    @Test
    fun `when all metrics are optimal, overallScore reaches 100`() {
        val score = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = true,
            hasActiveGoals = true,
            hasActiveRecurringTransactions = true,
            goalContributionsThisMonth = 2000.0,
            activeGoalsMonthlyTarget = 2000.0,
            globalBudgetAmount = 5000.0,
            totalExpensesThisMonth = 2500.0 // 50% spent -> 100 control
        )

        assertEquals(100.0, score.planningScore, 0.001)
        assertEquals(100.0, score.savingScore, 0.001)
        assertEquals(100.0, score.controlScore, 0.001)
        assertEquals(100, score.overallScore)
    }

    private fun assertFalse(condition: Boolean) = org.junit.Assert.assertFalse(condition)
}

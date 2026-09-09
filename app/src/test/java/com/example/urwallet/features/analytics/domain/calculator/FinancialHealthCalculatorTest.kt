package com.example.urwallet.features.analytics.domain.calculator

import com.example.urwallet.features.analytics.domain.model.HealthRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialHealthCalculatorTest {

    @Test
    fun `when user has no active goals, savingScore evaluates income savings rate or 0`() {
        val score = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = true,
            hasActiveGoals = false,
            hasActiveRecurringTransactions = true,
            goalContributionsThisMonth = 0.0,
            activeGoalsMonthlyTarget = 0.0,
            globalBudgetAmount = 5000.0,
            totalExpensesThisMonth = 2000.0,
            totalIncomeThisMonth = 0.0
        )

        assertEquals(0, score.savingScore)
        // Planning: 40 (budget) + 0 (no goals) + 30 (recurring) = 70
        assertEquals(70, score.planningScore)
        // Control: expenses 2000 / 5000 = 40% <= 50% -> full 100
        assertEquals(100, score.controlScore)
        // Overall: 70 * 0.35 + 0 * 0.35 + 100 * 0.30 = 24.5 + 30 = 54.5 -> 55
        assertEquals(55, score.overallScore)
        assertEquals(HealthRating.NEEDS_IMPROVEMENT, score.rating)
    }

    @Test
    fun `when monthlyTarget is zero or negative, savingScore falls back without division by zero`() {
        val score = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = false,
            hasActiveGoals = true,
            hasActiveRecurringTransactions = false,
            goalContributionsThisMonth = 500.0,
            activeGoalsMonthlyTarget = 0.0,
            globalBudgetAmount = 0.0,
            totalExpensesThisMonth = 0.0
        )

        assertEquals(0, score.savingScore)
        assertTrue(score.overallScore in 0..100)
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

        assertEquals(50, score.controlScore)
        assertEquals(0, score.planningScore)
        assertEquals(0, score.savingScore)
        // 0 * 0.35 + 0 * 0.35 + 50 * 0.30 = 15
        assertEquals(15, score.overallScore)
        assertEquals(HealthRating.NEEDS_IMPROVEMENT, score.rating)
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

        // 100 - (150 - 50) * 2 = 100 - 200 = -100 -> clamped to 0
        assertEquals(0, score.controlScore)
    }

    @Test
    fun `when all metrics are optimal, overallScore reaches 100 and rating is EXCELLENT`() {
        val score = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = true,
            hasActiveGoals = true,
            hasActiveRecurringTransactions = true,
            goalContributionsThisMonth = 2000.0,
            activeGoalsMonthlyTarget = 2000.0,
            globalBudgetAmount = 5000.0,
            totalExpensesThisMonth = 2500.0 // 50% spent -> 100 control
        )

        assertEquals(100, score.planningScore)
        assertEquals(100, score.savingScore)
        assertEquals(100, score.controlScore)
        assertEquals(100, score.overallScore)
        assertEquals(HealthRating.EXCELLENT, score.rating)
        assertTrue(score.coachingMessage.contains("ممتازة"))
    }

    @Test
    fun `coaching message targets weakest pillar`() {
        // Weakest: Saving
        val weakSaving = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = true,
            hasActiveGoals = true,
            hasActiveRecurringTransactions = true,
            goalContributionsThisMonth = 0.0,
            activeGoalsMonthlyTarget = 2000.0,
            globalBudgetAmount = 5000.0,
            totalExpensesThisMonth = 2500.0
        )
        assertTrue(weakSaving.coachingMessage.contains("الادخار"))

        // Weakest: Control (budget exceeded)
        val weakControl = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = true,
            hasActiveGoals = true,
            hasActiveRecurringTransactions = true,
            goalContributionsThisMonth = 2000.0,
            activeGoalsMonthlyTarget = 2000.0,
            globalBudgetAmount = 2000.0,
            totalExpensesThisMonth = 3500.0
        )
        assertTrue(weakControl.coachingMessage.contains("سقف الميزانية") || weakControl.coachingMessage.contains("نفقاتك"))
    }

    @Test
    fun `when no data at all, hasSufficientData is false`() {
        val score = FinancialHealthCalculator.calculate(
            hasGlobalBudgetThisMonth = false,
            hasActiveGoals = false,
            hasActiveRecurringTransactions = false,
            goalContributionsThisMonth = 0.0,
            activeGoalsMonthlyTarget = 0.0,
            globalBudgetAmount = 0.0,
            totalExpensesThisMonth = 0.0,
            totalIncomeThisMonth = 0.0
        )

        assertFalse(score.hasSufficientData)
        assertTrue(score.coachingMessage.contains("أضف بعض المعاملات"))
    }
}

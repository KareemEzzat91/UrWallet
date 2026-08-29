package com.example.urwallet.features.analytics.domain.calculator

import com.example.urwallet.features.analytics.domain.model.FinancialHealthScore
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Single source of truth for Financial Health Score calculation (0–100).
 *
 * Deterministic rules with comprehensive edge-case protection:
 *
 * 1. PlanningScore (0–100):
 *    - Has global budget set this month: +40 pts
 *    - Has at least one active goal: +30 pts
 *    - Uses active recurring transactions: +30 pts
 *
 * 2. SavingScore (0–100):
 *    - If monthlyTarget <= 0.0 or hasActiveGoals == false: 0.0 (fallback)
 *    - Else: (goalContributionsThisMonth / monthlyTarget) * 100, clamped to 0..100
 *
 * 3. ControlScore (0–100):
 *    - If no budget set (or budget <= 0.0): 50.0 (neutral fallback)
 *    - If budget set:
 *        globalBudgetPct = (totalExpensesThisMonth / budgetAmount) * 100
 *        100 - max(0.0, (globalBudgetPct - 50.0) * 2.0), clamped to 0..100
 *        (Full control 100 at <=50% spent; 0 at 100%+)
 *
 * Overall Score = round((PlanningScore + SavingScore + ControlScore) / 3.0)
 * Guaranteed: No NaN, no Infinity, no division by zero.
 */
object FinancialHealthCalculator {

    fun calculate(
        hasGlobalBudgetThisMonth: Boolean,
        hasActiveGoals: Boolean,
        hasActiveRecurringTransactions: Boolean,
        goalContributionsThisMonth: Double,
        activeGoalsMonthlyTarget: Double,
        globalBudgetAmount: Double,
        totalExpensesThisMonth: Double
    ): FinancialHealthScore {
        // 1. Planning Score
        var planning = 0.0
        if (hasGlobalBudgetThisMonth && globalBudgetAmount > 0.0) planning += 40.0
        if (hasActiveGoals) planning += 30.0
        if (hasActiveRecurringTransactions) planning += 30.0
        val planningScore = planning.coerceIn(0.0, 100.0)

        // 2. Saving Score (Edge cases: no goals, 0 monthly target, negative values)
        val safeMonthlyTarget = activeGoalsMonthlyTarget.coerceAtLeast(0.0)
        val safeContributions = goalContributionsThisMonth.coerceAtLeast(0.0)
        val savingScore = if (!hasActiveGoals || safeMonthlyTarget <= 0.0) {
            0.0
        } else {
            ((safeContributions / safeMonthlyTarget) * 100.0).coerceIn(0.0, 100.0)
        }

        // 3. Control Score (Edge cases: no budget, budget <= 0, no expenses)
        val safeBudget = globalBudgetAmount.coerceAtLeast(0.0)
        val safeExpenses = totalExpensesThisMonth.coerceAtLeast(0.0)
        val controlScore = if (!hasGlobalBudgetThisMonth || safeBudget <= 0.0) {
            50.0 // Neutral fallback when no budget is defined
        } else {
            val globalBudgetPct = (safeExpenses / safeBudget) * 100.0
            val penalty = max(0.0, (globalBudgetPct - 50.0) * 2.0)
            (100.0 - penalty).coerceIn(0.0, 100.0)
        }

        // Overall Score (0–100)
        val overall = ((planningScore + savingScore + controlScore) / 3.0).roundToInt().coerceIn(0, 100)

        return FinancialHealthScore(
            overallScore = overall,
            planningScore = planningScore,
            savingScore = savingScore,
            controlScore = controlScore
        )
    }
}

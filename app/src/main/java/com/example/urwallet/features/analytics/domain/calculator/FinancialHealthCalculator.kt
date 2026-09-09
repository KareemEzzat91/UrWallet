package com.example.urwallet.features.analytics.domain.calculator

import com.example.urwallet.features.analytics.domain.model.FinancialHealthScore
import com.example.urwallet.features.analytics.domain.model.HealthRating
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Single source of truth for Financial Health Score calculation (0–100).
 *
 * Deterministic rules with offline edge-case protection:
 *
 * 1. PlanningScore (0–100, Weight: 35%):
 *    - Has global budget set this month: +40 pts
 *    - Has at least one active goal: +30 pts
 *    - Uses active recurring transactions: +30 pts
 *
 * 2. SavingScore (0–100, Weight: 35%):
 *    - If active goals exist and monthlyTarget > 0:
 *        (goalContributionsThisMonth / activeGoalsMonthlyTarget) * 100, clamped 0..100
 *    - Else if totalIncome > 0 and net savings > 0:
 *        ((netSavings / totalIncome) / 0.20 * 100), clamped 0..100 (20% benchmark)
 *    - Else: 0.0
 *
 * 3. ControlScore (0–100, Weight: 30%):
 *    - If no budget set (or budget <= 0.0): 50.0 (neutral fallback)
 *    - If budget set:
 *        globalBudgetPct = (totalExpensesThisMonth / budgetAmount) * 100
 *        100 - max(0.0, (globalBudgetPct - 50.0) * 2.0), clamped 0..100
 *        (Full control 100 at <=50% spent; 0 at 100%+)
 *
 * Overall Score = round(Planning * 0.35 + Saving * 0.35 + Control * 0.30)
 */
object FinancialHealthCalculator {

    const val PLANNING_WEIGHT = 0.35
    const val SAVING_WEIGHT = 0.35
    const val CONTROL_WEIGHT = 0.30

    fun calculate(
        hasGlobalBudgetThisMonth: Boolean,
        hasActiveGoals: Boolean,
        hasActiveRecurringTransactions: Boolean,
        goalContributionsThisMonth: Double,
        activeGoalsMonthlyTarget: Double,
        globalBudgetAmount: Double,
        totalExpensesThisMonth: Double,
        totalIncomeThisMonth: Double = 0.0
    ): FinancialHealthScore {
        val hasSufficientData = hasGlobalBudgetThisMonth ||
                hasActiveGoals ||
                totalExpensesThisMonth > 0.0 ||
                totalIncomeThisMonth > 0.0

        // 1. Planning Score (0–100)
        var planning = 0.0
        if (hasGlobalBudgetThisMonth && globalBudgetAmount > 0.0) planning += 40.0
        if (hasActiveGoals) planning += 30.0
        if (hasActiveRecurringTransactions) planning += 30.0
        val planningScore = planning.coerceIn(0.0, 100.0)

        // 2. Saving Score (0–100)
        val safeMonthlyTarget = activeGoalsMonthlyTarget.coerceAtLeast(0.0)
        val safeContributions = goalContributionsThisMonth.coerceAtLeast(0.0)
        val netSavings = (totalIncomeThisMonth - totalExpensesThisMonth).coerceAtLeast(0.0)

        val savingScore = when {
            hasActiveGoals && safeMonthlyTarget > 0.0 -> {
                ((safeContributions / safeMonthlyTarget) * 100.0).coerceIn(0.0, 100.0)
            }
            totalIncomeThisMonth > 0.0 && netSavings > 0.0 -> {
                val savingsRate = netSavings / totalIncomeThisMonth
                // Standard 20% savings rate yields 100 points
                ((savingsRate / 0.20) * 100.0).coerceIn(0.0, 100.0)
            }
            else -> 0.0
        }

        // 3. Control Score (0–100)
        val safeBudget = globalBudgetAmount.coerceAtLeast(0.0)
        val safeExpenses = totalExpensesThisMonth.coerceAtLeast(0.0)
        val controlScore = if (!hasGlobalBudgetThisMonth || safeBudget <= 0.0) {
            50.0 // Neutral fallback when no budget is defined
        } else {
            val globalBudgetPct = (safeExpenses / safeBudget) * 100.0
            val penalty = max(0.0, (globalBudgetPct - 50.0) * 2.0)
            (100.0 - penalty).coerceIn(0.0, 100.0)
        }

        // Overall Score (0–100) using configured weights
        val weightedScore = (planningScore * PLANNING_WEIGHT) +
                (savingScore * SAVING_WEIGHT) +
                (controlScore * CONTROL_WEIGHT)
        val overall = weightedScore.roundToInt().coerceIn(0, 100)

        // Rating
        val rating = when (overall) {
            in 90..100 -> HealthRating.EXCELLENT
            in 75..89 -> HealthRating.VERY_GOOD
            in 60..74 -> HealthRating.AVERAGE
            else -> HealthRating.NEEDS_IMPROVEMENT
        }

        // Coaching message based on weakest pillar
        val coachingMessage = determineCoachingMessage(
            overall = overall,
            planning = planningScore,
            saving = savingScore,
            control = controlScore,
            hasSufficientData = hasSufficientData
        )

        return FinancialHealthScore(
            overallScore = overall,
            planningScore = planningScore.roundToInt(),
            savingScore = savingScore.roundToInt(),
            controlScore = controlScore.roundToInt(),
            rating = rating,
            coachingMessage = coachingMessage,
            hasSufficientData = hasSufficientData
        )
    }

    private fun determineCoachingMessage(
        overall: Int,
        planning: Double,
        saving: Double,
        control: Double,
        hasSufficientData: Boolean
    ): String {
        if (!hasSufficientData) {
            return "أضف بعض المعاملات والميزانيات لنبدأ بتحليل وضعك المالي بدقة."
        }

        if (overall >= 90) {
            return "صحتك المالية ممتازة! واصل الالتزام بخطتك وأهدافك."
        }

        // Find weakest pillar
        return when {
            planning <= saving && planning <= control -> {
                "حاول تحديد ميزانيات واضحة للفئات الأساسية لتعزيز تخطيطك المالي."
            }
            saving <= planning && saving <= control -> {
                "حاول زيادة نسبة الادخار أو المساهمة في أهدافك بانتظام."
            }
            else -> {
                "راقب نفقاتك اليومية وتجنب تجاوز سقف الميزانية."
            }
        }
    }
}

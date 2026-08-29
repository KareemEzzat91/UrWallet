package com.example.urwallet.features.budgets.domain.calculator

import com.example.urwallet.core.common.BudgetStatus
import com.example.urwallet.core.common.Constants

object BudgetCalculator {

    fun calculatePercentage(spent: Double, budgetAmount: Double): Double {
        if (budgetAmount <= 0.0) return 0.0
        return (spent / budgetAmount) * 100.0
    }

    fun calculateRemaining(spent: Double, budgetAmount: Double): Double {
        return (budgetAmount - spent).coerceAtLeast(0.0)
    }

    fun determineStatus(
        spent: Double,
        budgetAmount: Double,
        alertThreshold: Double = Constants.DEFAULT_ALERT_THRESHOLD
    ): BudgetStatus {
        if (budgetAmount <= 0.0) return BudgetStatus.HEALTHY
        val percentage = (spent / budgetAmount)
        val threshold = if (alertThreshold <= 0.0) Constants.DEFAULT_ALERT_THRESHOLD else alertThreshold

        return when {
            percentage >= 1.0 -> BudgetStatus.EXCEEDED
            percentage >= threshold -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.HEALTHY
        }
    }

    fun evaluate(
        spent: Double,
        budgetAmount: Double,
        alertThreshold: Double = Constants.DEFAULT_ALERT_THRESHOLD
    ): BudgetCalculationResult {
        val percentage = calculatePercentage(spent, budgetAmount)
        val remaining = calculateRemaining(spent, budgetAmount)
        val status = determineStatus(spent, budgetAmount, alertThreshold)

        return BudgetCalculationResult(
            spent = spent,
            budgetAmount = budgetAmount,
            remainingAmount = remaining,
            percentage = percentage,
            status = status
        )
    }
}

data class BudgetCalculationResult(
    val spent: Double,
    val budgetAmount: Double,
    val remainingAmount: Double,
    val percentage: Double,
    val status: BudgetStatus
)

package com.example.urwallet.features.goals.domain.calculator

/**
 * Single source of truth for Goal financial calculations.
 *
 * Rules:
 * - savedAmount = SUM(all contributions for goal)
 * - remainingAmount = max(targetAmount - savedAmount, 0.0)
 * - progress = savedAmount / targetAmount (clamped between 0.0 and 1.0)
 * - progressPercentage = progress * 100.0 (clamped between 0.0 and 100.0)
 * - isCompleted = targetAmount > 0 && savedAmount >= targetAmount
 */
object GoalCalculator {

    fun calculateSavedAmount(contributionsSum: Double?): Double {
        return (contributionsSum ?: 0.0).coerceAtLeast(0.0)
    }

    fun calculateRemainingAmount(targetAmount: Double, savedAmount: Double): Double {
        return (targetAmount - savedAmount).coerceAtLeast(0.0)
    }

    fun calculateProgress(targetAmount: Double, savedAmount: Double): Double {
        if (targetAmount <= 0.0) return 0.0
        return (savedAmount / targetAmount).coerceIn(0.0, 1.0)
    }

    fun calculateProgressPercentage(targetAmount: Double, savedAmount: Double): Double {
        return calculateProgress(targetAmount, savedAmount) * 100.0
    }

    fun isGoalCompleted(targetAmount: Double, savedAmount: Double): Boolean {
        return targetAmount > 0.0 && savedAmount >= targetAmount
    }
}

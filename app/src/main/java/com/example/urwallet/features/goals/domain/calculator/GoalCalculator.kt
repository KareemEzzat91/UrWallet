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

    fun calculateDaysRemaining(deadline: Long, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val diff = deadline - currentTimeMillis
        return if (diff <= 0) 0L else diff / (1000L * 60 * 60 * 24)
    }

    fun calculateMonthsRemaining(deadline: Long, currentTimeMillis: Long = System.currentTimeMillis()): Int {
        if (deadline <= currentTimeMillis) return 0
        val startCal = java.util.Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val endCal = java.util.Calendar.getInstance().apply { timeInMillis = deadline }
        val yearDiff = endCal.get(java.util.Calendar.YEAR) - startCal.get(java.util.Calendar.YEAR)
        val monthDiff = endCal.get(java.util.Calendar.MONTH) - startCal.get(java.util.Calendar.MONTH)
        val totalMonths = yearDiff * 12 + monthDiff
        return if (totalMonths <= 0) 1 else totalMonths
    }

    fun calculateRequiredMonthlySavings(
        targetAmount: Double,
        savedAmount: Double,
        deadline: Long,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Double {
        val remaining = calculateRemainingAmount(targetAmount, savedAmount)
        if (remaining <= 0.0) return 0.0
        val months = calculateMonthsRemaining(deadline, currentTimeMillis)
        return if (months > 0) remaining / months else remaining
    }
}

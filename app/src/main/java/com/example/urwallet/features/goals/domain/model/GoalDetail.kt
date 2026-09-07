package com.example.urwallet.features.goals.domain.model

data class GoalDetail(
    val goal: Goal,
    val savedAmount: Double,
    val targetAmount: Double,
    val remainingAmount: Double,
    val progressPercentage: Double,
    val isCompleted: Boolean,
    val deadline: Long,
    val daysRemaining: Long,
    val requiredMonthlySavings: Double,
    val contributions: List<GoalContribution>
)

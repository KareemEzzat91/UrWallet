package com.example.urwallet.features.goals.domain.model

import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.features.goals.domain.calculator.GoalCalculator

data class Goal(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val paceMode: GoalPaceMode,
    val monthlyTarget: Double,
    val deadline: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val remainingAmount: Double
        get() = GoalCalculator.calculateRemainingAmount(targetAmount, savedAmount)

    val progressPercentage: Double
        get() = GoalCalculator.calculateProgressPercentage(targetAmount, savedAmount)

    val isCompleted: Boolean
        get() = GoalCalculator.isGoalCompleted(targetAmount, savedAmount)
}


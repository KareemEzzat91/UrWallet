package com.example.urwallet.features.goals.domain.model

import com.example.urwallet.core.common.GoalPaceMode

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
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false
) {
    val remainingAmount: Double
        get() = (targetAmount - savedAmount).coerceAtLeast(0.0)

    val progressPercentage: Double
        get() = if (targetAmount > 0) ((savedAmount / targetAmount) * 100.0).coerceIn(0.0, 100.0) else 0.0
}

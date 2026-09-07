package com.example.urwallet.features.goals.domain.model

data class GoalSummary(
    val id: Long,
    val name: String,
    val icon: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val remainingAmount: Double,
    val progressPercentage: Double,
    val isCompleted: Boolean,
    val deadline: Long,
    val daysRemaining: Long
)

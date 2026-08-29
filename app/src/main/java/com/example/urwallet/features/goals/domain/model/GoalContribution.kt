package com.example.urwallet.features.goals.domain.model

data class GoalContribution(
    val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val note: String? = null,
    val date: Long = System.currentTimeMillis()
)

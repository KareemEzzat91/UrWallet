package com.example.urwallet.features.goals.domain.model

data class GoalsListResult(
    val activeGoals: List<Goal>,
    val completedGoals: List<Goal>
)

package com.example.urwallet.features.goals.presentation

import com.example.urwallet.features.goals.domain.model.Goal

sealed interface GoalsUiState {
    data object Loading : GoalsUiState
    data class Success(
        val activeGoals: List<Goal>,
        val completedGoals: List<Goal>
    ) : GoalsUiState
    data object Empty : GoalsUiState
    data class Error(val message: String) : GoalsUiState
}

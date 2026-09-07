package com.example.urwallet.features.goals.presentation.detail

import com.example.urwallet.features.goals.domain.model.GoalDetail

sealed interface GoalDetailUiState {
    data object Loading : GoalDetailUiState
    data class Success(val detail: GoalDetail) : GoalDetailUiState
    data class Error(val message: String) : GoalDetailUiState
}

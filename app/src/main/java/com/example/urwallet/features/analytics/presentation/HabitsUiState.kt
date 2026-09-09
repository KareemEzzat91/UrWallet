package com.example.urwallet.features.analytics.presentation

import com.example.urwallet.features.analytics.domain.model.FinancialHabitsResult

sealed interface HabitsUiState {
    data object Loading : HabitsUiState
    data class Success(val result: FinancialHabitsResult) : HabitsUiState
    data object Empty : HabitsUiState
    data class Error(val message: String) : HabitsUiState
}

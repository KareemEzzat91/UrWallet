package com.example.urwallet.features.budgets.presentation

import com.example.urwallet.features.budgets.domain.model.BudgetsSummaryResult

sealed interface BudgetsUiState {
    data object Loading : BudgetsUiState
    data class Success(val result: BudgetsSummaryResult) : BudgetsUiState
    data object Empty : BudgetsUiState
    data class Error(val message: String) : BudgetsUiState
}

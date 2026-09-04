package com.example.urwallet.features.dashboard.presentation

import com.example.urwallet.features.dashboard.domain.model.DashboardSummary

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val summary: DashboardSummary) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

package com.example.urwallet.features.analytics.presentation

import com.example.urwallet.features.analytics.domain.model.MonthlyAnalyticsResult

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Success(val result: MonthlyAnalyticsResult) : AnalyticsUiState
    data object Empty : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}

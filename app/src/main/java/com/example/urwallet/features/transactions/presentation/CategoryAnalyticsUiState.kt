package com.example.urwallet.features.transactions.presentation

import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.CategoryAnalytics

sealed interface CategoryAnalyticsUiState {
    data object Loading : CategoryAnalyticsUiState
    data class Success(
        val analytics: CategoryAnalytics,
        val allCategories: List<Category>
    ) : CategoryAnalyticsUiState
    data object Empty : CategoryAnalyticsUiState
    data class Error(val message: String) : CategoryAnalyticsUiState
}

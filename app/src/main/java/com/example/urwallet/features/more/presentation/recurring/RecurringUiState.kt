package com.example.urwallet.features.more.presentation.recurring

import com.example.urwallet.features.more.domain.model.RecurringTransactionWithCategory

enum class RecurringFilterType {
    ALL,
    EXPENSE,
    INCOME
}

data class RecurringUiState(
    val isLoading: Boolean = true,
    val items: List<RecurringTransactionWithCategory> = emptyList(),
    val filteredItems: List<RecurringTransactionWithCategory> = emptyList(),
    val monthlyObligations: Double = 0.0,
    val monthlyRecurringIncome: Double = 0.0,
    val activeSubscriptionsCount: Int = 0,
    val filterType: RecurringFilterType = RecurringFilterType.ALL,
    val errorMessage: String? = null
)

sealed interface RecurringUiEvent {
    data class ShowMessage(val message: String) : RecurringUiEvent
    data object DismissAddSheet : RecurringUiEvent
}

package com.example.urwallet.features.transactions.presentation

import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data object Empty : TransactionsUiState
    data class NoSearchResults(
        val query: String,
        val hasActiveFilters: Boolean
    ) : TransactionsUiState
    data class Success(
        val items: List<TransactionListItem>,
        val totalIncome: Double = 0.0,
        val totalExpense: Double = 0.0,
        val hasActiveFilters: Boolean = false,
        val activeFilterCount: Int = 0
    ) : TransactionsUiState
    data class Error(val message: String) : TransactionsUiState
}

sealed interface TransactionListItem {
    data class Header(val dateLabel: String) : TransactionListItem
    data class Item(
        val transaction: Transaction,
        val category: Category? = null
    ) : TransactionListItem
}

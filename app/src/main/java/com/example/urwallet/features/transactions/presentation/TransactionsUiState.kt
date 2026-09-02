package com.example.urwallet.features.transactions.presentation

import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data object Empty : TransactionsUiState
    data class Success(
        val items: List<TransactionListItem>
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

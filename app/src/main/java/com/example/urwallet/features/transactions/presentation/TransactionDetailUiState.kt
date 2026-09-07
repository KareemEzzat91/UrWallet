package com.example.urwallet.features.transactions.presentation

import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction

sealed interface TransactionDetailUiState {
    data object Loading : TransactionDetailUiState
    data class Success(
        val transaction: Transaction,
        val category: Category?
    ) : TransactionDetailUiState
    data object Deleted : TransactionDetailUiState
    data class Error(val message: String) : TransactionDetailUiState
}

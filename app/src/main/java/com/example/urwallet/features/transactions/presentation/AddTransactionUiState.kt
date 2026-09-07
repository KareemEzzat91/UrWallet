package com.example.urwallet.features.transactions.presentation

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category

data class AddTransactionUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    /** True when no categories exist for the currently selected transaction type. */
    val noCategoriesAvailable: Boolean get() = categories.isEmpty()
}

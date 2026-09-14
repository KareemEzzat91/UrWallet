package com.example.urwallet.features.more.domain.model

data class RecurringTransactionWithCategory(
    val recurring: RecurringTransaction,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String
)

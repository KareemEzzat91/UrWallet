package com.example.urwallet.features.transactions.domain.model

data class CategoryAnalytics(
    val category: Category,
    val totalSpent: Double,
    val previousPeriodSpent: Double,
    val percentageChange: Double?,
    val averageAmount: Double,
    val transactionCount: Int,
    val topTransactions: List<Transaction>,
    val budgetStatus: CategoryBudgetStatus? = null,
    val insightMessage: String? = null
)

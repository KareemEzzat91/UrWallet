package com.example.urwallet.features.budgets.domain.model

data class BudgetsSummaryResult(
    val globalBudget: BudgetSummary?,
    val categoryBudgets: List<BudgetSummary>,
    val totalBudgetLimit: Double,
    val totalSpent: Double,
    val month: Int,
    val year: Int
) {
    val isEmpty: Boolean get() = globalBudget == null && categoryBudgets.isEmpty()
}

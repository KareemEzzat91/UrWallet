package com.example.urwallet.features.budgets.domain.model

import com.example.urwallet.core.common.BudgetStatus

data class BudgetSummary(
    val budgetId: Long,
    val categoryId: Long? = null,
    val categoryName: String,
    val categoryIcon: String,
    val limitAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progressPercentage: Double,
    val visualProgress: Int,
    val status: BudgetStatus,
    val month: Int,
    val year: Int
) {
    val isGlobal: Boolean get() = categoryId == null
    val isExceeded: Boolean get() = status == BudgetStatus.EXCEEDED
    val overspentAmount: Double get() = if (remainingAmount < 0.0) -remainingAmount else 0.0
}

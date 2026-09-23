package com.example.urwallet.features.budgets.domain.model

import com.example.urwallet.core.common.BudgetStatus

data class BudgetsSummaryResult(
    val globalBudget: BudgetSummary?,
    val categoryBudgets: List<BudgetSummary>,
    val totalBudgetLimit: Double,
    val totalSpent: Double,
    val month: Int,
    val year: Int
) {
    val isEmpty: Boolean get() = globalBudget == null && categoryBudgets.isEmpty()
    val totalBudgetsCount: Int get() = (if (globalBudget != null) 1 else 0) + categoryBudgets.size
    val healthyBudgetsCount: Int get() = (if (globalBudget?.status == BudgetStatus.HEALTHY) 1 else 0) + categoryBudgets.count { it.status == BudgetStatus.HEALTHY }
    val nearLimitBudgetsCount: Int get() = (if (globalBudget?.status == BudgetStatus.NEAR_LIMIT) 1 else 0) + categoryBudgets.count { it.status == BudgetStatus.NEAR_LIMIT }
    val exceededBudgetsCount: Int get() = (if (globalBudget?.status == BudgetStatus.EXCEEDED) 1 else 0) + categoryBudgets.count { it.status == BudgetStatus.EXCEEDED }
    val remainingBudget: Double get() = totalBudgetLimit - totalSpent
    val overallProgressPercentage: Double get() = if (totalBudgetLimit > 0.0) (totalSpent / totalBudgetLimit) * 100.0 else 0.0
}

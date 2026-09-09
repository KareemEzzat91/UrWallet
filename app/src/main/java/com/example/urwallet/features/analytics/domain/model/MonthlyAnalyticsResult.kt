package com.example.urwallet.features.analytics.domain.model

data class MonthlyAnalyticsResult(
    val month: Int,
    val year: Int,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netSavings: Double,
    val savingsRate: Double,
    val dailySpending: Map<Int, Double>,
    val categoryBreakdown: List<CategorySpendingSummary>,
    val healthScore: FinancialHealthScore
) {
    val hasData: Boolean get() = totalIncome > 0.0 || totalExpenses > 0.0 || categoryBreakdown.isNotEmpty()
}

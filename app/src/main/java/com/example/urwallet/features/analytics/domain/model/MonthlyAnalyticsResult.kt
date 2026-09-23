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
    val healthScore: FinancialHealthScore,
    val period: AnalyticsTimePeriod = AnalyticsTimePeriod.THIS_MONTH,
    val expenseToIncomeRatio: Double = 0.0,
    val budgetUtilization: Double = 0.0,
    val totalGoalSavings: Double = 0.0,
    val cashFlowComparison: List<MonthlyCashFlow> = emptyList(),
    val dailyExpensePoints: List<DailyExpensePoint> = emptyList()
) {
    val hasData: Boolean get() = totalIncome > 0.0 || totalExpenses > 0.0 || categoryBreakdown.isNotEmpty()
}

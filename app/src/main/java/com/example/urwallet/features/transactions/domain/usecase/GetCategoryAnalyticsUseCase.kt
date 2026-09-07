package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.budgets.domain.calculator.BudgetCalculationResult
import com.example.urwallet.features.budgets.domain.calculator.BudgetCalculator
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.CategoryAnalytics
import com.example.urwallet.features.transactions.domain.model.CategoryBudgetStatus
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetCategoryAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {

    operator fun invoke(
        categoryId: Long,
        month: Int = DateUtils.getCurrentMonth(),
        year: Int = DateUtils.getCurrentYear()
    ): Flow<CategoryAnalytics?> {
        val startOfMonth = DateUtils.getStartOfMonth(month, year)
        val endOfMonth = DateUtils.getEndOfMonth(month, year)

        val (prevMonth, prevYear) = DateUtils.getPreviousMonth(month, year)
        val startOfPrevMonth = DateUtils.getStartOfMonth(prevMonth, prevYear)
        val endOfPrevMonth = DateUtils.getEndOfMonth(prevMonth, prevYear)

        return combine(
            categoryRepository.getAllCategories(),
            transactionRepository.getTransactionsByCategoryAndPeriod(categoryId, startOfMonth, endOfMonth),
            transactionRepository.getSumByCategoryAndPeriod(categoryId, startOfPrevMonth, endOfPrevMonth),
            budgetRepository.getBudgetForCategory(categoryId, month, year)
        ) { categories, currentTransactions, prevSum, budget ->
            val category = categories.find { it.id == categoryId } ?: return@combine null

            val expenseTransactions = currentTransactions.filter { it.type == TransactionType.EXPENSE }
            val totalSpent = expenseTransactions.sumOf { it.amount }

            val transactionCount = expenseTransactions.size
            val averageAmount = if (transactionCount > 0) totalSpent / transactionCount else 0.0

            val percentageChange = if (prevSum > 0.0) {
                ((totalSpent - prevSum) / prevSum) * 100.0
            } else {
                null
            }

            val topTransactions = expenseTransactions
                .sortedByDescending { it.amount }
                .take(5)

            val budgetCalculation = if (budget != null && budget.amount > 0.0) {
                BudgetCalculator.evaluate(
                    spent = totalSpent,
                    budgetAmount = budget.amount,
                    alertThreshold = budget.alertThreshold
                )
            } else {
                null
            }

            val budgetStatus = if (budget != null) {
                CategoryBudgetStatus(budget = budget, calculation = budgetCalculation)
            } else {
                null
            }

            val insight = generateInsight(
                category = category,
                totalSpent = totalSpent,
                percentageChange = percentageChange,
                budgetCalculation = budgetCalculation
            )

            CategoryAnalytics(
                category = category,
                totalSpent = totalSpent,
                previousPeriodSpent = prevSum,
                percentageChange = percentageChange,
                averageAmount = averageAmount,
                transactionCount = transactionCount,
                topTransactions = topTransactions,
                budgetStatus = budgetStatus,
                insightMessage = insight
            )
        }
    }

    private fun generateInsight(
        category: Category,
        totalSpent: Double,
        percentageChange: Double?,
        budgetCalculation: BudgetCalculationResult?
    ): String {
        return when {
            budgetCalculation != null && budgetCalculation.percentage >= 100.0 -> {
                "تنبيه: تم تجاوز ميزانية ${category.name} بنسبة ${budgetCalculation.percentage.toInt()}%. حاول تقليل المصروفات في هذا التصنيف."
            }
            budgetCalculation != null && budgetCalculation.percentage >= 80.0 -> {
                val remainingStr = Formatters.formatCurrency(budgetCalculation.remainingAmount)
                "تنبيه: اقتربت من ميزانية ${category.name}. المتبقي $remainingStr فقط."
            }
            percentageChange != null && percentageChange > 20.0 -> {
                "مصروفات ${category.name} زادت بنسبة ${percentageChange.toInt()}% عن الشهر الماضي. حاول تقليل المصاريف في هذا التصنيف هذا الأسبوع."
            }
            percentageChange != null && percentageChange < -10.0 -> {
                "أحسنت! مصروفات ${category.name} انخفضت بنسبة ${(-percentageChange).toInt()}% مقارنة بالشهر الماضي. استمر على هذا الأداء! 🎉"
            }
            totalSpent > 0.0 -> {
                val totalStr = Formatters.formatCurrency(totalSpent)
                "إجمالي ما تم إنفاقه على ${category.name} هذا الشهر هو $totalStr."
            }
            else -> {
                "لا توجد مصاريف مسجلة في تصنيف ${category.name} لهذا الشهر حتى الآن."
            }
        }
    }
}

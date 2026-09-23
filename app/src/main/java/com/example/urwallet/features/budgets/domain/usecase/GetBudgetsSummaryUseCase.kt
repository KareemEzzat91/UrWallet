package com.example.urwallet.features.budgets.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.budgets.domain.calculator.BudgetCalculator
import com.example.urwallet.features.budgets.domain.model.BudgetSummary
import com.example.urwallet.features.budgets.domain.model.BudgetsSummaryResult
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetBudgetsSummaryUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    operator fun invoke(
        month: Int = DateUtils.getCurrentMonth(),
        year: Int = DateUtils.getCurrentYear()
    ): Flow<BudgetsSummaryResult> {
        val startOfMonth = DateUtils.getStartOfMonth(month, year)
        val endOfMonth = DateUtils.getEndOfMonth(month, year)

        val globalBudgetFlow = budgetRepository.getGlobalBudget(month, year)
        val categoryBudgetsFlow = budgetRepository.getCategoryBudgets(month, year)
        val totalExpenseFlow = transactionRepository.getSumByTypeAndPeriod(
            type = TransactionType.EXPENSE,
            startDate = startOfMonth,
            endDate = endOfMonth
        )
        val categorySpendingFlow = transactionRepository.getCategorySpendingBetween(startOfMonth, endOfMonth)
        val categoriesFlow = categoryRepository.getAllCategories()

        return combine(
            globalBudgetFlow,
            categoryBudgetsFlow,
            totalExpenseFlow,
            categorySpendingFlow,
            categoriesFlow
        ) { globalBudget, categoryBudgets, totalExpenseSpending, categorySpendingMap, categories ->
            val categoriesMap = categories.associateBy { it.id }

            // Build Global Budget summary if defined
            val globalSummary = globalBudget?.let { gb ->
                val calc = BudgetCalculator.evaluate(
                    spent = totalExpenseSpending,
                    budgetAmount = gb.amount,
                    alertThreshold = gb.alertThreshold
                )
                BudgetSummary(
                    budgetId = gb.id,
                    categoryId = null,
                    categoryName = "الميزانية العامة",
                    categoryIcon = "ic_bills",
                    limitAmount = gb.amount,
                    spentAmount = calc.spent,
                    remainingAmount = calc.remainingAmount,
                    progressPercentage = calc.percentage,
                    visualProgress = calc.visualProgress,
                    status = calc.status,
                    month = month,
                    year = year
                )
            }

            // Build Category Budget summaries
            val categorySummaries = categoryBudgets.map { cb ->
                val spent = cb.categoryId?.let { categorySpendingMap[it] } ?: 0.0
                val calc = BudgetCalculator.evaluate(
                    spent = spent,
                    budgetAmount = cb.amount,
                    alertThreshold = cb.alertThreshold
                )
                val cat = cb.categoryId?.let { categoriesMap[it] }
                BudgetSummary(
                    budgetId = cb.id,
                    categoryId = cb.categoryId,
                    categoryName = cat?.name ?: "فئة غير معروفة",
                    categoryIcon = cat?.icon ?: "ic_other",
                    limitAmount = cb.amount,
                    spentAmount = calc.spent,
                    remainingAmount = calc.remainingAmount,
                    progressPercentage = calc.percentage,
                    visualProgress = calc.visualProgress,
                    status = calc.status,
                    month = month,
                    year = year
                )
            }.sortedByDescending { it.progressPercentage }

            val totalLimit = (globalBudget?.amount ?: 0.0) + categoryBudgets.sumOf { it.amount }

            BudgetsSummaryResult(
                globalBudget = globalSummary,
                categoryBudgets = categorySummaries,
                totalBudgetLimit = totalLimit,
                totalSpent = totalExpenseSpending,
                month = month,
                year = year
            )
        }
    }
}

package com.example.urwallet.features.analytics.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.analytics.domain.calculator.FinancialHealthCalculator
import com.example.urwallet.features.analytics.domain.model.CategorySpendingSummary
import com.example.urwallet.features.analytics.domain.model.MonthlyAnalyticsResult
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetMonthlyAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val recurringRepository: RecurringRepository
) {

    operator fun invoke(
        month: Int = DateUtils.getCurrentMonth(),
        year: Int = DateUtils.getCurrentYear()
    ): Flow<MonthlyAnalyticsResult> {
        val startOfMonth = DateUtils.getStartOfMonth(month, year)
        val endOfMonth = DateUtils.getEndOfMonth(month, year)
        val daysInMonth = DateUtils.getDaysInMonth(month, year)

        val transactionsFlow = transactionRepository.getTransactionsBetween(startOfMonth, endOfMonth)
        val categoriesFlow = categoryRepository.getAllCategories()
        val globalBudgetFlow = budgetRepository.getGlobalBudget(month, year)
        val activeGoalsFlow = goalRepository.getActiveGoals()
        val goalContributionsFlow = goalRepository.getMonthlyContributionsSum(startOfMonth, endOfMonth)
        val activeRecurringFlow = recurringRepository.getActiveRecurringTransactions()

        return combine(
            transactionsFlow,
            categoriesFlow,
            globalBudgetFlow,
            activeGoalsFlow,
            goalContributionsFlow,
            activeRecurringFlow
        ) { args: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val transactions = args[0] as List<Transaction>
            @Suppress("UNCHECKED_CAST")
            val categories = args[1] as List<Category>
            val globalBudget = args[2] as? Budget
            @Suppress("UNCHECKED_CAST")
            val activeGoals = args[3] as List<Goal>
            val goalContributions = args[4] as Double
            @Suppress("UNCHECKED_CAST")
            val activeRecurring = args[5] as List<RecurringTransaction>

            val categoriesMap = categories.associateBy { it.id }

            // Strictly filter by transaction type
            val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
            val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }

            val totalIncome = incomeTransactions.sumOf { it.amount }
            val totalExpenses = expenseTransactions.sumOf { it.amount }
            val netSavings = totalIncome - totalExpenses

            val savingsRate = if (totalIncome > 0.0) {
                (netSavings / totalIncome) * 100.0
            } else {
                0.0
            }

            // Daily spending map for the entire month (1..daysInMonth)
            val dailySpendingMap = (1..daysInMonth).associateWith { 0.0 }.toMutableMap()
            for (tx in expenseTransactions) {
                val day = DateUtils.getDayOfMonth(tx.date)
                if (day in 1..daysInMonth) {
                    dailySpendingMap[day] = (dailySpendingMap[day] ?: 0.0) + tx.amount
                }
            }

            // Category breakdown sorted descending by spending amount
            val categoryExpensesMap = expenseTransactions.groupBy { it.categoryId }
            val categoryBreakdown = categoryExpensesMap.map { (catId, catTxs) ->
                val cat = categoriesMap[catId]
                val amount = catTxs.sumOf { it.amount }
                val percentage = if (totalExpenses > 0.0) {
                    (amount / totalExpenses) * 100.0
                } else {
                    0.0
                }
                CategorySpendingSummary(
                    categoryId = catId,
                    categoryName = cat?.name ?: "أخرى",
                    categoryIcon = cat?.icon ?: "ic_other",
                    categoryColor = cat?.color ?: "#78909C",
                    amount = amount,
                    percentage = percentage
                )
            }.sortedByDescending { it.amount }

            // Financial Health Score
            val hasGlobalBudget = globalBudget != null && globalBudget.amount > 0.0
            val globalBudgetAmount = globalBudget?.amount ?: 0.0
            val hasActiveGoals = activeGoals.isNotEmpty()
            val activeGoalsTarget = activeGoals.sumOf { it.monthlyTarget }
            val hasActiveRecurring = activeRecurring.isNotEmpty()

            val healthScore = FinancialHealthCalculator.calculate(
                hasGlobalBudgetThisMonth = hasGlobalBudget,
                hasActiveGoals = hasActiveGoals,
                hasActiveRecurringTransactions = hasActiveRecurring,
                goalContributionsThisMonth = goalContributions,
                activeGoalsMonthlyTarget = activeGoalsTarget,
                globalBudgetAmount = globalBudgetAmount,
                totalExpensesThisMonth = totalExpenses,
                totalIncomeThisMonth = totalIncome
            )

            MonthlyAnalyticsResult(
                month = month,
                year = year,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netSavings = netSavings,
                savingsRate = savingsRate,
                dailySpending = dailySpendingMap,
                categoryBreakdown = categoryBreakdown,
                healthScore = healthScore
            )
        }
    }
}

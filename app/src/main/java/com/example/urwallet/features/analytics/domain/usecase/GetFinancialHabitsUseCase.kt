package com.example.urwallet.features.analytics.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.analytics.domain.calculator.HabitsCalculator
import com.example.urwallet.features.analytics.domain.model.FinancialHabitsResult
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetFinancialHabitsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository
) {

    operator fun invoke(
        month: Int = DateUtils.getCurrentMonth(),
        year: Int = DateUtils.getCurrentYear()
    ): Flow<FinancialHabitsResult> {
        val startOfMonth = DateUtils.getStartOfMonth(month, year)
        val endOfMonth = DateUtils.getEndOfMonth(month, year)

        val transactionsFlow = transactionRepository.getTransactionsBetween(startOfMonth, endOfMonth)
        val categoriesFlow = categoryRepository.getAllCategories()
        val globalBudgetFlow = budgetRepository.getGlobalBudget(month, year)
        val activeGoalsFlow = goalRepository.getActiveGoals()
        val goalContributionsFlow = goalRepository.getMonthlyContributionsSum(startOfMonth, endOfMonth)

        return combine(
            transactionsFlow,
            categoriesFlow,
            globalBudgetFlow,
            activeGoalsFlow,
            goalContributionsFlow
        ) { transactions, categories, globalBudget, activeGoals, goalContributions ->
            val categoriesMap = categories.associateBy { it.id }

            val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
            val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }

            val totalExpenses = expenseTransactions.sumOf { it.amount }
            val totalIncome = incomeTransactions.sumOf { it.amount }
            val netSavings = totalIncome - totalExpenses

            val savingsRate = if (totalIncome > 0.0) {
                (netSavings / totalIncome) * 100.0
            } else {
                0.0
            }

            // 1. Peak spending day
            val (peakDay, peakAmount) = HabitsCalculator.calculatePeakSpendingDay(expenseTransactions)

            // 2. Daily average spending
            val dailyAverage = HabitsCalculator.calculateDailyAverage(totalExpenses, month, year)

            // 3. 50/30/20 breakdown
            val rule50_30_20 = HabitsCalculator.calculateRule50_30_20(
                expenseTransactions = expenseTransactions,
                categoriesMap = categoriesMap,
                goalContributions = goalContributions,
                totalIncome = totalIncome
            )

            // 4. Financial personality
            val hasGlobalBudget = globalBudget != null && globalBudget.amount > 0.0
            val isBudgetExceeded = globalBudget != null && totalExpenses > globalBudget.amount
            val hasActiveGoals = activeGoals.isNotEmpty()

            val personality = HabitsCalculator.determinePersonality(
                savingsRate = savingsRate,
                hasGlobalBudget = hasGlobalBudget,
                isBudgetExceeded = isBudgetExceeded,
                wantsPercentage = rule50_30_20.wantsPercentage,
                hasActiveGoals = hasActiveGoals,
                goalContributions = goalContributions
            )

            FinancialHabitsResult(
                personality = personality,
                peakSpendingDay = peakDay,
                peakSpendingAmount = peakAmount,
                dailyAverageSpend = dailyAverage,
                rule50_30_20 = rule50_30_20,
                month = month,
                year = year
            )
        }
    }
}

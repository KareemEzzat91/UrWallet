package com.example.urwallet.features.dashboard.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.challenges.domain.usecase.GetChallengesUseCase
import com.example.urwallet.features.dashboard.domain.model.DashboardSummary
import com.example.urwallet.features.dashboard.domain.model.DashboardTransactionItem
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val getChallengesUseCase: GetChallengesUseCase
) {

    operator fun invoke(): Flow<DashboardSummary> {
        val currentMonth = DateUtils.getCurrentMonth()
        val currentYear = DateUtils.getCurrentYear()
        val startOfMonth = DateUtils.getStartOfMonth(currentMonth, currentYear)
        val endOfMonth = DateUtils.getEndOfMonth(currentMonth, currentYear)

        val totalIncomeFlow = transactionRepository.getTotalSumByType(TransactionType.INCOME)
        val totalExpenseFlow = transactionRepository.getTotalSumByType(TransactionType.EXPENSE)
        val monthlyIncomeFlow = transactionRepository.getSumByTypeAndPeriod(
            TransactionType.INCOME, startOfMonth, endOfMonth
        )
        val monthlyExpenseFlow = transactionRepository.getSumByTypeAndPeriod(
            TransactionType.EXPENSE, startOfMonth, endOfMonth
        )
        val recentTransactionsFlow = transactionRepository.getRecentTransactions(4)
        val categoriesFlow = transactionRepository.getAllCategories()
        val nearestGoalFlow = goalRepository.getNearestActiveGoal()
        val challengesFlow = getChallengesUseCase()

        val financialTotalsFlow = combine(
            totalIncomeFlow,
            totalExpenseFlow,
            monthlyIncomeFlow,
            monthlyExpenseFlow
        ) { totalIncome, totalExpense, monthlyIncome, monthlyExpense ->
            FinancialTotals(
                netBalance = totalIncome - totalExpense,
                monthlyIncome = monthlyIncome,
                monthlyExpense = monthlyExpense
            )
        }

        val recentWithCategoryFlow = combine(
            recentTransactionsFlow,
            categoriesFlow
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { transaction ->
                DashboardTransactionItem(
                    transaction = transaction,
                    category = categoryMap[transaction.categoryId]
                )
            }
        }

        return combine(
            financialTotalsFlow,
            recentWithCategoryFlow,
            nearestGoalFlow,
            challengesFlow
        ) { totals, recentItems, nearestGoal, challengesResult ->
            DashboardSummary(
                netBalance = totals.netBalance,
                monthlyIncome = totals.monthlyIncome,
                monthlyExpense = totals.monthlyExpense,
                recentTransactions = recentItems,
                nearestGoal = nearestGoal,
                activeChallenge = challengesResult.activeChallenges.firstOrNull()
            )
        }
    }

    private data class FinancialTotals(
        val netBalance: Double,
        val monthlyIncome: Double,
        val monthlyExpense: Double
    )
}

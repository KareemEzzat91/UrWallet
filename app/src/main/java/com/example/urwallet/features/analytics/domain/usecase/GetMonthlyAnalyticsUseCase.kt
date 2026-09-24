package com.example.urwallet.features.analytics.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.analytics.domain.calculator.FinancialHealthCalculator
import com.example.urwallet.features.analytics.domain.model.AnalyticsTimePeriod
import com.example.urwallet.features.analytics.domain.model.CategorySpendingSummary
import com.example.urwallet.features.analytics.domain.model.DailyExpensePoint
import com.example.urwallet.features.analytics.domain.model.MonthlyAnalyticsResult
import com.example.urwallet.features.analytics.domain.model.MonthlyCashFlow
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.data.entity.DailySpendingEntity
import com.example.urwallet.features.transactions.data.entity.MonthlyCashFlowEntity
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
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
        return invoke(AnalyticsTimePeriod.THIS_MONTH, month, year)
    }

    operator fun invoke(
        period: AnalyticsTimePeriod,
        month: Int = DateUtils.getCurrentMonth(),
        year: Int = DateUtils.getCurrentYear()
    ): Flow<MonthlyAnalyticsResult> {
        val (startDate, endDate) = DateUtils.getPeriodDateRange(period, month, year)
        val daysInMonth = DateUtils.getDaysInMonth(month, year)

        val incomeSumFlow = transactionRepository.getSumByTypeAndPeriod(TransactionType.INCOME, startDate, endDate)
        val expenseSumFlow = transactionRepository.getSumByTypeAndPeriod(TransactionType.EXPENSE, startDate, endDate)
        val categorySpendingFlow = transactionRepository.getCategorySpendingBetween(startDate, endDate)
        val monthlyCashFlowsFlow = transactionRepository.getMonthlyCashFlowsBetween(startDate, endDate)
        val dailySpendingFlow = transactionRepository.getDailyExpensesBetween(startDate, endDate)
        val categoriesFlow = categoryRepository.getAllCategories()
        val globalBudgetFlow = budgetRepository.getGlobalBudget(month, year)
        val activeGoalsFlow = goalRepository.getActiveGoals()
        val goalContributionsFlow = goalRepository.getMonthlyContributionsSum(startDate, endDate)
        val activeRecurringFlow = recurringRepository.getActiveRecurringTransactions()

        return combine(
            incomeSumFlow,
            expenseSumFlow,
            categorySpendingFlow,
            monthlyCashFlowsFlow,
            dailySpendingFlow,
            categoriesFlow,
            globalBudgetFlow,
            activeGoalsFlow,
            goalContributionsFlow,
            activeRecurringFlow
        ) { args: Array<Any?> ->
            val totalIncome = args[0] as Double
            val totalExpenses = args[1] as Double
            @Suppress("UNCHECKED_CAST")
            val categorySpendingMap = args[2] as Map<Long, Double>
            @Suppress("UNCHECKED_CAST")
            val monthlyCashFlowEntities = args[3] as List<MonthlyCashFlowEntity>
            @Suppress("UNCHECKED_CAST")
            val dailySpendingEntities = args[4] as List<DailySpendingEntity>
            @Suppress("UNCHECKED_CAST")
            val categories = args[5] as List<Category>
            val globalBudget = args[6] as? Budget
            @Suppress("UNCHECKED_CAST")
            val activeGoals = args[7] as List<Goal>
            val goalContributions = args[8] as Double
            @Suppress("UNCHECKED_CAST")
            val activeRecurring = args[9] as List<RecurringTransaction>

            val categoriesMap = categories.associateBy { it.id }

            val netSavings = totalIncome - totalExpenses
            val savingsRate = if (totalIncome > 0.0) {
                (netSavings / totalIncome) * 100.0
            } else {
                0.0
            }

            val expenseToIncomeRatio = if (totalIncome > 0.0) {
                (totalExpenses / totalIncome) * 100.0
            } else if (totalExpenses > 0.0) {
                100.0
            } else {
                0.0
            }

            val budgetUtilization = if (globalBudget != null && globalBudget.amount > 0.0) {
                (totalExpenses / globalBudget.amount) * 100.0
            } else {
                0.0
            }

            val dailySpendingMap = (1..daysInMonth).associateWith { 0.0 }.toMutableMap()
            dailySpendingEntities.forEach { entity ->
                if (entity.month == month && entity.day in 1..daysInMonth) {
                    dailySpendingMap[entity.day] = (dailySpendingMap[entity.day] ?: 0.0) + entity.totalAmount
                }
            }

            val dailyExpensePoints = dailySpendingEntities.map { entity ->
                val cal = Calendar.getInstance().apply {
                    set(entity.year, entity.month - 1, entity.day, 0, 0, 0)
                }
                DailyExpensePoint(
                    date = cal.timeInMillis,
                    dayNumber = entity.day,
                    label = "${entity.day}",
                    amount = entity.totalAmount
                )
            }

            val categoryBreakdown = categorySpendingMap.map { (catId, amount) ->
                val cat = categoriesMap[catId]
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

            val monthYearGroups = monthlyCashFlowEntities.groupBy { Pair(it.year, it.month) }
            val cashFlowComparison = if (monthYearGroups.isNotEmpty()) {
                monthYearGroups.map { (ym, flows) ->
                    val (y, m) = ym
                    val inc = flows.firstOrNull { it.type == TransactionType.INCOME }?.totalAmount ?: 0.0
                    val exp = flows.firstOrNull { it.type == TransactionType.EXPENSE }?.totalAmount ?: 0.0
                    MonthlyCashFlow(
                        monthName = DateUtils.formatMonthYearLocalized(m, y),
                        month = m,
                        year = y,
                        income = inc,
                        expense = exp,
                        net = inc - exp
                    )
                }.sortedWith(compareBy({ it.year }, { it.month }))
            } else {
                listOf(
                    MonthlyCashFlow(
                        monthName = DateUtils.formatMonthYearLocalized(month, year),
                        month = month,
                        year = year,
                        income = totalIncome,
                        expense = totalExpenses,
                        net = netSavings
                    )
                )
            }

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
                healthScore = healthScore,
                period = period,
                expenseToIncomeRatio = expenseToIncomeRatio,
                budgetUtilization = budgetUtilization,
                totalGoalSavings = goalContributions,
                cashFlowComparison = cashFlowComparison,
                dailyExpensePoints = dailyExpensePoints
            )
        }
    }
}

package com.example.urwallet.features.analytics.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class GetMonthlyAnalyticsUseCaseTest {

    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var fakeRecurringRepository: FakeRecurringRepository
    private lateinit var useCase: GetMonthlyAnalyticsUseCase

    private class FakeTransactionRepository : TransactionRepository {
        var transactions = listOf<Transaction>()
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(transactions)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(transactions.find { it.id == id })
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(transactions.take(limit))
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(transactions)
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(transactions.filter { it.categoryId == categoryId })
        override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> =
            flowOf(transactions.filter { it.type == type }.sumOf { it.amount })
        override fun getTotalSumByType(type: TransactionType): Flow<Double> =
            flowOf(transactions.filter { it.type == type }.sumOf { it.amount })
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> =
            flowOf(0.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(transactions.size)
        override suspend fun insertTransaction(transaction: Transaction): Long = 1L
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: Long): Category? = null
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    private class FakeCategoryRepository : CategoryRepository {
        var categories = listOf<Category>()
        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
            flowOf(categories.filter { it.type == type })
        override suspend fun getCategoryById(id: Long): Category? = categories.find { it.id == id }
        override suspend fun insertCategory(category: Category): Long = category.id
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    private class FakeBudgetRepository : BudgetRepository {
        var globalBudget: Budget? = null
        override fun getGlobalBudget(month: Int, year: Int): Flow<Budget?> = flowOf(globalBudget)
        override fun getCategoryBudgets(month: Int, year: Int): Flow<List<Budget>> = flowOf(emptyList())
        override fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<Budget?> = flowOf(null)
        override suspend fun getGlobalBudgetSync(month: Int, year: Int): Budget? = globalBudget
        override suspend fun getBudgetForCategorySync(categoryId: Long, month: Int, year: Int): Budget? = null
        override suspend fun insertOrUpdateBudget(budget: Budget): Long = 1L
        override suspend fun deleteBudget(id: Long) {}
    }

    private class FakeGoalRepository : GoalRepository {
        var activeGoals = listOf<Goal>()
        var monthlyContributions = 0.0
        override fun getAllGoals(): Flow<List<Goal>> = flowOf(activeGoals)
        override fun getActiveGoals(): Flow<List<Goal>> = flowOf(activeGoals)
        override fun getNearestActiveGoal(): Flow<Goal?> = flowOf(activeGoals.firstOrNull())
        override fun getGoalById(id: Long): Flow<Goal?> = flowOf(activeGoals.find { it.id == id })
        override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> = flowOf(emptyList())
        override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> = flowOf(monthlyContributions)
        override suspend fun insertGoal(goal: Goal): Long = 1L
        override suspend fun updateGoal(goal: Goal) {}
        override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long = 1L
        override suspend fun deleteGoal(id: Long) {}
    }

    private class FakeRecurringRepository : RecurringRepository {
        var recurring = listOf<RecurringTransaction>()
        override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(recurring)
        override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(recurring)
        override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long = 1L
        override suspend fun updateRecurringTransaction(recurring: RecurringTransaction) {}
        override suspend fun deleteRecurringTransaction(id: Long) {}
        override suspend fun toggleActive(id: Long, isActive: Boolean) {}
    }

    @Before
    fun setUp() {
        fakeTransactionRepository = FakeTransactionRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakeBudgetRepository = FakeBudgetRepository()
        fakeGoalRepository = FakeGoalRepository()
        fakeRecurringRepository = FakeRecurringRepository()

        useCase = GetMonthlyAnalyticsUseCase(
            transactionRepository = fakeTransactionRepository,
            categoryRepository = fakeCategoryRepository,
            budgetRepository = fakeBudgetRepository,
            goalRepository = fakeGoalRepository,
            recurringRepository = fakeRecurringRepository
        )
    }

    @Test
    fun `when month has no transactions, result reflects zero amounts and hasData is false`() = runTest {
        val result = useCase(month = 9, year = 2026).first()

        assertEquals(0.0, result.totalIncome, 0.001)
        assertEquals(0.0, result.totalExpenses, 0.001)
        assertEquals(0.0, result.netSavings, 0.001)
        assertEquals(0.0, result.savingsRate, 0.001)
        assertTrue(result.categoryBreakdown.isEmpty())
        assertFalse(result.hasData)
    }

    @Test
    fun `evaluates income, expenses, and net savings correctly`() = runTest {
        val calDay5 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 5) }
        val calDay12 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 12) }

        val catFood = Category(id = 1L, name = "طعام", type = CategoryType.EXPENSE, icon = "ic_food", color = "#FF7043")
        val catBills = Category(id = 2L, name = "فواتير", type = CategoryType.EXPENSE, icon = "ic_bills", color = "#FFA726")
        fakeCategoryRepository.categories = listOf(catFood, catBills)

        val transactions = listOf(
            Transaction(id = 1L, amount = 10000.0, type = TransactionType.INCOME, categoryId = 99L, title = "راتب", date = calDay5.timeInMillis),
            Transaction(id = 2L, amount = 3000.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "سوبرماركت", date = calDay5.timeInMillis),
            Transaction(id = 3L, amount = 1000.0, type = TransactionType.EXPENSE, categoryId = 2L, title = "كهرباء", date = calDay12.timeInMillis)
        )
        fakeTransactionRepository.transactions = transactions

        val result = useCase(month = 9, year = 2026).first()

        assertEquals(10000.0, result.totalIncome, 0.001)
        assertEquals(4000.0, result.totalExpenses, 0.001)
        assertEquals(6000.0, result.netSavings, 0.001)
        assertEquals(60.0, result.savingsRate, 0.001) // (6000 / 10000) * 100
        assertTrue(result.hasData)

        // Verify Daily trend has expenses on Day 5 and Day 12
        assertEquals(3000.0, result.dailySpending[5] ?: 0.0, 0.001)
        assertEquals(1000.0, result.dailySpending[12] ?: 0.0, 0.001)
        assertEquals(0.0, result.dailySpending[1] ?: 0.0, 0.001)

        // Verify Category Breakdown sorted descending
        assertEquals(2, result.categoryBreakdown.size)
        assertEquals("طعام", result.categoryBreakdown[0].categoryName)
        assertEquals(3000.0, result.categoryBreakdown[0].amount, 0.001)
        assertEquals(75.0, result.categoryBreakdown[0].percentage, 0.001) // 3000 / 4000

        assertEquals("فواتير", result.categoryBreakdown[1].categoryName)
        assertEquals(1000.0, result.categoryBreakdown[1].amount, 0.001)
        assertEquals(25.0, result.categoryBreakdown[1].percentage, 0.001) // 1000 / 4000
    }
}

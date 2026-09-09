package com.example.urwallet.features.analytics.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.analytics.domain.model.PersonalityType
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class GetFinancialHabitsUseCaseTest {

    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var useCase: GetFinancialHabitsUseCase

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

    @Before
    fun setUp() {
        fakeTransactionRepository = FakeTransactionRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakeBudgetRepository = FakeBudgetRepository()
        fakeGoalRepository = FakeGoalRepository()

        useCase = GetFinancialHabitsUseCase(
            transactionRepository = fakeTransactionRepository,
            categoryRepository = fakeCategoryRepository,
            budgetRepository = fakeBudgetRepository,
            goalRepository = fakeGoalRepository
        )
    }

    @Test
    fun `evaluates habits, peak day, and 50-30-20 rule correctly`() = runTest {
        val calSat = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 5) }

        val catBills = Category(id = 1L, name = "فواتير", type = CategoryType.EXPENSE, icon = "ic_bills", color = "#FFA726")
        val catShop = Category(id = 2L, name = "تسوق", type = CategoryType.EXPENSE, icon = "ic_shopping", color = "#AB47BC")
        fakeCategoryRepository.categories = listOf(catBills, catShop)

        val transactions = listOf(
            Transaction(id = 1L, amount = 10000.0, type = TransactionType.INCOME, categoryId = 99L, title = "راتب", date = calSat.timeInMillis),
            Transaction(id = 2L, amount = 5000.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "إيجار", date = calSat.timeInMillis),
            Transaction(id = 3L, amount = 3000.0, type = TransactionType.EXPENSE, categoryId = 2L, title = "تسوق", date = calSat.timeInMillis)
        )
        fakeTransactionRepository.transactions = transactions
        fakeGoalRepository.monthlyContributions = 2000.0
        fakeGoalRepository.activeGoals = listOf(
            Goal(id = 1L, name = "طوارئ", icon = "ic_goal_emergency", targetAmount = 50000.0, savedAmount = 2000.0, paceMode = GoalPaceMode.BALANCED, monthlyTarget = 2000.0, deadline = 1000000L)
        )
        fakeBudgetRepository.globalBudget = Budget(id = 1L, categoryId = null, amount = 9000.0, month = 9, year = 2026)

        val result = useCase(month = 9, year = 2026).first()

        assertEquals("السبت", result.peakSpendingDay)
        assertEquals(8000.0, result.peakSpendingAmount, 0.001)

        // 50/30/20: Needs=5000 (50%), Wants=3000 (30%), Savings=2000 (20%)
        assertEquals(5000.0, result.rule50_30_20.needsAmount, 0.001)
        assertEquals(3000.0, result.rule50_30_20.wantsAmount, 0.001)
        assertEquals(2000.0, result.rule50_30_20.savingsAmount, 0.001)

        assertEquals(50.0, result.rule50_30_20.needsPercentage, 0.001)
        assertEquals(30.0, result.rule50_30_20.wantsPercentage, 0.001)
        assertEquals(20.0, result.rule50_30_20.savingsPercentage, 0.001)

        // Personality: Smart Planner
        assertEquals(PersonalityType.SMART_PLANNER, result.personality.type)
        assertNotNull(result.personality.coachRecommendation)
    }
}

package com.example.urwallet.features.dashboard

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.dashboard.domain.usecase.GetDashboardSummaryUseCase
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetDashboardSummaryUseCaseTest {

    private lateinit var fakeTransactionRepository: DashboardFakeTransactionRepository
    private lateinit var fakeGoalRepository: DashboardFakeGoalRepository
    private lateinit var useCase: GetDashboardSummaryUseCase

    @Before
    fun setUp() {
        fakeTransactionRepository = DashboardFakeTransactionRepository()
        fakeGoalRepository = DashboardFakeGoalRepository()
        useCase = GetDashboardSummaryUseCase(
            transactionRepository = fakeTransactionRepository,
            goalRepository = fakeGoalRepository
        )
    }

    @Test
    fun `when repositories are empty, returns zero values and null goal`() = runTest {
        val summary = useCase().first()

        assertEquals(0.0, summary.netBalance, 0.001)
        assertEquals(0.0, summary.monthlyIncome, 0.001)
        assertEquals(0.0, summary.monthlyExpense, 0.001)
        assertEquals(0, summary.recentTransactions.size)
        assertNull(summary.nearestGoal)
    }

    @Test
    fun `when transactions exist, calculates correct net balance and monthly totals`() = runTest {
        fakeTransactionRepository.totalIncome = 25000.0
        fakeTransactionRepository.totalExpense = 8500.0
        fakeTransactionRepository.monthlyIncome = 15000.0
        fakeTransactionRepository.monthlyExpense = 4500.0

        val summary = useCase().first()

        assertEquals(16500.0, summary.netBalance, 0.001) // 25000 - 8500
        assertEquals(15000.0, summary.monthlyIncome, 0.001)
        assertEquals(4500.0, summary.monthlyExpense, 0.001)
    }

    @Test
    fun `when nearest active goal exists, returns it in summary`() = runTest {
        val testGoal = Goal(
            id = 1L,
            name = "صندوق الطوارئ",
            icon = "🛡️",
            targetAmount = 10000.0,
            savedAmount = 4000.0,
            paceMode = GoalPaceMode.BALANCED,
            monthlyTarget = 1000.0,
            deadline = System.currentTimeMillis() + 1000000L
        )
        fakeGoalRepository.nearestGoal = testGoal

        val summary = useCase().first()

        assertNotNull(summary.nearestGoal)
        assertEquals("صندوق الطوارئ", summary.nearestGoal?.name)
        assertEquals(4000.0, summary.nearestGoal?.savedAmount ?: 0.0, 0.001)
        assertEquals(6000.0, summary.nearestGoal?.remainingAmount ?: 0.0, 0.001)
        assertEquals(40.0, summary.nearestGoal?.progressPercentage ?: 0.0, 0.001)
    }

    @Test
    fun `recent transactions are mapped with matching category details`() = runTest {
        val category = Category(
            id = 1L,
            name = "أكل ومشروبات",
            type = CategoryType.EXPENSE,
            icon = "ic_food",
            color = "#FF5722"
        )
        fakeTransactionRepository.categories.add(category)

        val transaction = Transaction(
            id = 10L,
            amount = 120.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "غداء",
            date = System.currentTimeMillis()
        )
        fakeTransactionRepository.recentTransactions.add(transaction)

        val summary = useCase().first()

        assertEquals(1, summary.recentTransactions.size)
        val item = summary.recentTransactions.first()
        assertEquals("غداء", item.transaction.title)
        assertEquals("أكل ومشروبات", item.category?.name)
        assertEquals("ic_food", item.category?.icon)
    }
}

class DashboardFakeTransactionRepository : TransactionRepository {
    var totalIncome: Double = 0.0
    var totalExpense: Double = 0.0
    var monthlyIncome: Double = 0.0
    var monthlyExpense: Double = 0.0
    val recentTransactions = mutableListOf<Transaction>()
    val categories = mutableListOf<Category>()

    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(recentTransactions)
    override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(recentTransactions.find { it.id == id })
    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(recentTransactions.take(limit))
    override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())

    override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> {
        return flowOf(if (type == TransactionType.INCOME) monthlyIncome else monthlyExpense)
    }

    override fun getTotalSumByType(type: TransactionType): Flow<Double> {
        return flowOf(if (type == TransactionType.INCOME) totalIncome else totalExpense)
    }

    override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(0)
    override suspend fun insertTransaction(transaction: Transaction): Long = 1L
    override suspend fun updateTransaction(transaction: Transaction) {}
    override suspend fun deleteTransaction(transaction: Transaction) {}
    override suspend fun deleteTransactionById(id: Long) {}

    override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(categories.filter { it.type == type })
    override suspend fun getCategoryById(id: Long): Category? = categories.find { it.id == id }
    override suspend fun insertCategory(category: Category): Long = 1L
    override suspend fun updateCategory(category: Category) {}
    override suspend fun deleteCategory(id: Long) {}
}

class DashboardFakeGoalRepository : GoalRepository {
    var nearestGoal: Goal? = null
    val goals = mutableListOf<Goal>()

    override fun getAllGoals(): Flow<List<Goal>> = flowOf(goals)
    override fun getActiveGoals(): Flow<List<Goal>> = flowOf(goals.filter { !it.isCompleted })
    override fun getNearestActiveGoal(): Flow<Goal?> = flowOf(nearestGoal)
    override fun getGoalById(id: Long): Flow<Goal?> = flowOf(goals.find { it.id == id })
    override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> = flowOf(emptyList())
    override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override suspend fun insertGoal(goal: Goal): Long = 1L
    override suspend fun updateGoal(goal: Goal) {}
    override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long = 1L
    override suspend fun deleteGoal(id: Long) {}
}

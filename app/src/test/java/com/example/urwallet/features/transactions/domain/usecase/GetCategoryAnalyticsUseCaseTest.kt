package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.BudgetStatus
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetCategoryAnalyticsUseCaseTest {

    private lateinit var useCase: GetCategoryAnalyticsUseCase

    private val testCategory = Category(
        id = 1L,
        name = "الأكل",
        type = CategoryType.EXPENSE,
        icon = "ic_food",
        color = "#FF5722",
        isDefault = true
    )

    private val testBudget = Budget(
        id = 10L,
        categoryId = 1L,
        amount = 1000.0,
        month = 5,
        year = 2026,
        alertThreshold = 0.8
    )

    private val currentMonthTransactions = listOf(
        Transaction(1L, 300.0, TransactionType.EXPENSE, 1L, "مطعم 1", null, 1000L),
        Transaction(2L, 500.0, TransactionType.EXPENSE, 1L, "مطعم 2", null, 2000L),
        Transaction(3L, 200.0, TransactionType.EXPENSE, 1L, "مطعم 3", null, 3000L)
    )

    private val fakeCategoryRepo = object : CategoryRepository {
        override fun getAllCategories(): Flow<List<Category>> = flowOf(listOf(testCategory))
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(listOf(testCategory))
        override suspend fun getCategoryById(id: Long): Category? = if (id == 1L) testCategory else null
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    private val fakeBudgetRepo = object : BudgetRepository {
        override fun getGlobalBudget(month: Int, year: Int): Flow<Budget?> = flowOf(null)
        override fun getCategoryBudgets(month: Int, year: Int): Flow<List<Budget>> = flowOf(listOf(testBudget))
        override fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<Budget?> =
            flowOf(if (categoryId == 1L) testBudget else null)
        override suspend fun insertOrUpdateBudget(budget: Budget): Long = 10L
        override suspend fun deleteBudget(id: Long) {}
    }

    private fun createFakeTransactionRepo(
        transactions: List<Transaction> = currentMonthTransactions,
        prevMonthSum: Double = 800.0
    ) = object : TransactionRepository {
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
            flowOf(prevMonthSum)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(transactions.size)
        override suspend fun insertTransaction(transaction: Transaction): Long = 1L
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override fun getAllCategories(): Flow<List<Category>> = flowOf(listOf(testCategory))
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(listOf(testCategory))
        override suspend fun getCategoryById(id: Long): Category? = testCategory
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    @Before
    fun setUp() {
        useCase = GetCategoryAnalyticsUseCase(
            transactionRepository = createFakeTransactionRepo(),
            budgetRepository = fakeBudgetRepo,
            categoryRepository = fakeCategoryRepo
        )
    }

    @Test
    fun `calculates total spent, transaction count and average correctly`() = runBlocking {
        val result = useCase(categoryId = 1L, month = 5, year = 2026).first()

        assertNotNull(result)
        assertEquals(1000.0, result!!.totalSpent, 0.001)
        assertEquals(3, result.transactionCount)
        assertEquals(333.33, result.averageAmount, 0.01)
    }

    @Test
    fun `calculates percentage change compared to previous month`() = runBlocking {
        // totalSpent = 1000.0, prevSum = 800.0 -> +25%
        val result = useCase(categoryId = 1L, month = 5, year = 2026).first()

        assertNotNull(result)
        assertNotNull(result!!.percentageChange)
        assertEquals(25.0, result.percentageChange!!, 0.01)
    }

    @Test
    fun `percentage change is null when previous month spending is zero`() = runBlocking {
        val useCaseZeroPrev = GetCategoryAnalyticsUseCase(
            transactionRepository = createFakeTransactionRepo(prevMonthSum = 0.0),
            budgetRepository = fakeBudgetRepo,
            categoryRepository = fakeCategoryRepo
        )

        val result = useCaseZeroPrev(categoryId = 1L, month = 5, year = 2026).first()

        assertNotNull(result)
        assertNull(result!!.percentageChange)
    }

    @Test
    fun `top transactions are ordered descending by amount`() = runBlocking {
        val result = useCase(categoryId = 1L, month = 5, year = 2026).first()

        assertNotNull(result)
        val top = result!!.topTransactions
        assertEquals(3, top.size)
        assertEquals(500.0, top[0].amount, 0.001)
        assertEquals(300.0, top[1].amount, 0.001)
        assertEquals(200.0, top[2].amount, 0.001)
    }

    @Test
    fun `evaluates category budget correctly`() = runBlocking {
        val result = useCase(categoryId = 1L, month = 5, year = 2026).first()

        assertNotNull(result)
        val budgetStatus = result!!.budgetStatus
        assertNotNull(budgetStatus)
        assertNotNull(budgetStatus!!.calculation)
        // 1000.0 spent out of 1000.0 budget = 100% -> EXCEEDED
        assertEquals(BudgetStatus.EXCEEDED, budgetStatus.calculation!!.status)
    }

    @Test
    fun `returns null when category does not exist`() = runBlocking {
        val result = useCase(categoryId = 999L, month = 5, year = 2026).first()
        assertNull(result)
    }
}

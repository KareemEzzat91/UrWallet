package com.example.urwallet.features.budgets.domain.usecase

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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetBudgetsSummaryUseCaseTest {

    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var useCase: GetBudgetsSummaryUseCase

    private class FakeBudgetRepository : BudgetRepository {
        var globalBudget: Budget? = null
        var categoryBudgets: List<Budget> = emptyList()

        override fun getGlobalBudget(month: Int, year: Int): Flow<Budget?> = flowOf(globalBudget)
        override fun getCategoryBudgets(month: Int, year: Int): Flow<List<Budget>> = flowOf(categoryBudgets)
        override fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<Budget?> =
            flowOf(categoryBudgets.find { it.categoryId == categoryId })

        override suspend fun getGlobalBudgetSync(month: Int, year: Int): Budget? = globalBudget
        override suspend fun getBudgetForCategorySync(categoryId: Long, month: Int, year: Int): Budget? =
            categoryBudgets.find { it.categoryId == categoryId }

        override suspend fun insertOrUpdateBudget(budget: Budget): Long = 1L
        override suspend fun deleteBudget(id: Long) {}
    }

    private class FakeTransactionRepository : TransactionRepository {
        var transactions: List<Transaction> = emptyList()

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
        var categories: List<Category> = emptyList()

        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
            flowOf(categories.filter { it.type == type })
        override suspend fun getCategoryById(id: Long): Category? = categories.find { it.id == id }
        override suspend fun insertCategory(category: Category): Long = category.id
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    @Before
    fun setUp() {
        fakeBudgetRepository = FakeBudgetRepository()
        fakeTransactionRepository = FakeTransactionRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        useCase = GetBudgetsSummaryUseCase(fakeBudgetRepository, fakeTransactionRepository, fakeCategoryRepository)
    }

    @Test
    fun `when no budgets exist, summary result is empty`() = runTest {
        val result = useCase(month = 9, year = 2026).first()

        assertTrue(result.isEmpty)
        assertNull(result.globalBudget)
        assertTrue(result.categoryBudgets.isEmpty())
        assertEquals(0.0, result.totalSpent, 0.001)
    }

    @Test
    fun `when global budget exists, evaluate expenses and exclude income`() = runTest {
        val globalBudget = Budget(id = 1L, categoryId = null, amount = 10000.0, month = 9, year = 2026)
        val transactions = listOf(
            Transaction(id = 1L, amount = 3000.0, type = TransactionType.EXPENSE, categoryId = 2L, title = "مطعم", date = 1000L),
            Transaction(id = 2L, amount = 1500.0, type = TransactionType.EXPENSE, categoryId = 3L, title = "وقود", date = 2000L),
            Transaction(id = 3L, amount = 20000.0, type = TransactionType.INCOME, categoryId = 5L, title = "راتب", date = 3000L) // Income must be excluded!
        )

        fakeBudgetRepository.globalBudget = globalBudget
        fakeTransactionRepository.transactions = transactions

        val result = useCase(month = 9, year = 2026).first()

        assertNotNull(result.globalBudget)
        assertEquals(4500.0, result.totalSpent, 0.001) // 3000 + 1500
        assertEquals(4500.0, result.globalBudget!!.spentAmount, 0.001)
        assertEquals(5500.0, result.globalBudget!!.remainingAmount, 0.001)
        assertEquals(45.0, result.globalBudget!!.progressPercentage, 0.001)
        assertEquals(BudgetStatus.HEALTHY, result.globalBudget!!.status)
    }

    @Test
    fun `when category budget is exceeded, reflect negative remaining and EXCEEDED status`() = runTest {
        val categoryBudget = Budget(id = 2L, categoryId = 10L, amount = 2000.0, month = 9, year = 2026)
        val category = Category(id = 10L, name = "مطاعم", type = CategoryType.EXPENSE, icon = "ic_food", color = "#FF5722")
        val transactions = listOf(
            Transaction(id = 1L, amount = 2500.0, type = TransactionType.EXPENSE, categoryId = 10L, title = "عشاء", date = 1000L)
        )

        fakeBudgetRepository.categoryBudgets = listOf(categoryBudget)
        fakeCategoryRepository.categories = listOf(category)
        fakeTransactionRepository.transactions = transactions

        val result = useCase(month = 9, year = 2026).first()

        assertEquals(1, result.categoryBudgets.size)
        val cbSummary = result.categoryBudgets.first()
        assertEquals("مطاعم", cbSummary.categoryName)
        assertEquals("ic_food", cbSummary.categoryIcon)
        assertEquals(2500.0, cbSummary.spentAmount, 0.001)
        assertEquals(-500.0, cbSummary.remainingAmount, 0.001)
        assertEquals(500.0, cbSummary.overspentAmount, 0.001)
        assertEquals(100, cbSummary.visualProgress)
        assertEquals(BudgetStatus.EXCEEDED, cbSummary.status)
        assertTrue(cbSummary.isExceeded)
    }
}

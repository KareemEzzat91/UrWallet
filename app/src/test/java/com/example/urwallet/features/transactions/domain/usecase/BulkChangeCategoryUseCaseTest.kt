package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BulkChangeCategoryUseCaseTest {

    private lateinit var fakeTxRepo: TestTransactionRepository
    private lateinit var fakeCatRepo: TestCategoryRepository
    private lateinit var useCase: BulkChangeCategoryUseCase

    @Before
    fun setUp() {
        fakeTxRepo = TestTransactionRepository()
        fakeCatRepo = TestCategoryRepository()
        useCase = BulkChangeCategoryUseCase(fakeTxRepo, fakeCatRepo)
    }

    @Test
    fun `empty id set returns failure`() = runTest {
        val result = useCase(emptySet(), 1L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invalid category id returns failure`() = runTest {
        val result = useCase(setOf(1L, 2L), 0L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `non existent category returns failure`() = runTest {
        fakeCatRepo.category = null
        val result = useCase(setOf(1L, 2L), 99L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("غير موجود") == true)
    }

    @Test
    fun `valid ids and existing category updates transactions atomically and returns count`() = runTest {
        fakeCatRepo.category = Category(id = 5L, name = "طعام", type = CategoryType.EXPENSE, icon = "ic_food", color = "#FF5722")
        fakeTxRepo.updatedReturnCount = 4

        val result = useCase(setOf(1L, 2L, 3L, 4L), 5L)

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull())
        assertEquals(listOf(1L, 2L, 3L, 4L), fakeTxRepo.lastUpdatedIds?.sorted())
        assertEquals(5L, fakeTxRepo.lastUpdatedCategoryId)
    }

    private class TestTransactionRepository : TransactionRepository {
        var lastUpdatedIds: List<Long>? = null
        var lastUpdatedCategoryId: Long? = null
        var updatedReturnCount = 0

        override suspend fun updateCategoryByIds(ids: List<Long>, categoryId: Long): Int {
            lastUpdatedIds = ids
            lastUpdatedCategoryId = categoryId
            return updatedReturnCount
        }

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(null)
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTotalSumByType(type: TransactionType): Flow<Double> = flowOf(0.0)
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(0)
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

    private class TestCategoryRepository : CategoryRepository {
        var category: Category? = null

        override suspend fun getCategoryById(id: Long): Category? = category
        override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }
}

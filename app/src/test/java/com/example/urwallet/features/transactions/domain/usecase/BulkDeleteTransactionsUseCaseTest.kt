package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BulkDeleteTransactionsUseCaseTest {

    private lateinit var fakeRepo: TestTransactionRepository
    private lateinit var useCase: BulkDeleteTransactionsUseCase

    @Before
    fun setUp() {
        fakeRepo = TestTransactionRepository()
        useCase = BulkDeleteTransactionsUseCase(fakeRepo)
    }

    @Test
    fun `empty id set returns failure with error message`() = runTest {
        val result = useCase(emptySet())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `set with only non positive ids returns failure`() = runTest {
        val result = useCase(setOf(0L, -1L, -5L))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `valid id set calls repository deleteTransactionsByIds and returns count`() = runTest {
        fakeRepo.deletedReturnCount = 3
        val result = useCase(setOf(10L, 20L, 30L))

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
        assertEquals(listOf(10L, 20L, 30L), fakeRepo.lastDeletedIds?.sorted())
    }

    @Test
    fun `repository exception propagates as failure`() = runTest {
        fakeRepo.shouldThrow = true
        val result = useCase(setOf(1L, 2L))

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }

    private class TestTransactionRepository : TransactionRepository {
        var lastDeletedIds: List<Long>? = null
        var deletedReturnCount = 0
        var shouldThrow = false

        override suspend fun deleteTransactionsByIds(ids: List<Long>): Int {
            if (shouldThrow) throw IllegalStateException("DB error")
            lastDeletedIds = ids
            return deletedReturnCount
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
}

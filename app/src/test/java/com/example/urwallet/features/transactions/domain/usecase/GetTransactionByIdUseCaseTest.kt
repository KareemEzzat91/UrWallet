package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
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

class GetTransactionByIdUseCaseTest {

    private lateinit var useCase: GetTransactionByIdUseCase

    private val sampleTx = Transaction(
        id = 42L,
        amount = 120.0,
        type = TransactionType.EXPENSE,
        categoryId = 1L,
        title = "ستاربكس",
        note = "قهوة",
        date = 1000L
    )

    private val fakeRepo = object : TransactionRepository {
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(listOf(sampleTx))
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(if (id == 42L) sampleTx else null)
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(listOf(sampleTx))
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(listOf(sampleTx))
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(listOf(sampleTx))
        override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(120.0)
        override fun getTotalSumByType(type: TransactionType): Flow<Double> = flowOf(120.0)
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = flowOf(120.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(1)
        override suspend fun insertTransaction(transaction: Transaction): Long = 42L
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

    @Before
    fun setUp() {
        useCase = GetTransactionByIdUseCase(fakeRepo)
    }

    @Test
    fun `returns transaction when id exists`() = runTest {
        val result = useCase(42L).first()
        assertNotNull(result)
        assertEquals("ستاربكس", result!!.title)
        assertEquals(120.0, result.amount, 0.001)
    }

    @Test
    fun `returns null when id does not exist`() = runTest {
        val result = useCase(999L).first()
        assertNull(result)
    }
}

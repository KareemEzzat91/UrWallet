package com.example.urwallet.features.transactions

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import com.example.urwallet.features.transactions.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateTransactionUseCaseTest {

    private lateinit var fakeRepository: FakeUpdateTransactionRepository
    private lateinit var useCase: UpdateTransactionUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeUpdateTransactionRepository()
        useCase = UpdateTransactionUseCase(fakeRepository)
    }

    @Test
    fun `when id is invalid, returns failure`() = runTest {
        val resultZeroId = useCase(
            id = 0L,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "غداء",
            date = 1000L
        )
        assertTrue(resultZeroId.isFailure)

        val resultNegativeId = useCase(
            id = -5L,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "غداء",
            date = 1000L
        )
        assertTrue(resultNegativeId.isFailure)
    }

    @Test
    fun `when amount is zero or negative, returns failure`() = runTest {
        val resultZero = useCase(
            id = 1L,
            amount = 0.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "قهوة",
            date = 1000L
        )
        assertTrue(resultZero.isFailure)

        val resultNegative = useCase(
            id = 1L,
            amount = -50.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "قهوة",
            date = 1000L
        )
        assertTrue(resultNegative.isFailure)
    }

    @Test
    fun `when title is blank, returns failure`() = runTest {
        val resultBlank = useCase(
            id = 1L,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "   ",
            date = 1000L
        )
        assertTrue(resultBlank.isFailure)
    }

    @Test
    fun `when categoryId is invalid, returns failure`() = runTest {
        val resultInvalidCategory = useCase(
            id = 1L,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = 0L,
            title = "سوبرماركت",
            date = 1000L
        )
        assertTrue(resultInvalidCategory.isFailure)
    }

    @Test
    fun `when input is valid, updates transaction and preserves id and date`() = runTest {
        val initialTx = Transaction(
            id = 42L,
            amount = 50.0,
            type = TransactionType.EXPENSE,
            categoryId = 2L,
            title = "شاي",
            note = "القديم",
            date = 123456789L
        )
        fakeRepository.transactions.add(initialTx)

        val result = useCase(
            id = 42L,
            amount = 150.0,
            type = TransactionType.INCOME,
            categoryId = 5L,
            title = "مكافأة",
            note = "الجديد",
            date = 123456789L
        )

        assertTrue(result.isSuccess)
        val updated = fakeRepository.transactions.first { it.id == 42L }
        assertEquals(42L, updated.id)
        assertEquals(150.0, updated.amount, 0.001)
        assertEquals(TransactionType.INCOME, updated.type)
        assertEquals(5L, updated.categoryId)
        assertEquals("مكافأة", updated.title)
        assertEquals("الجديد", updated.note)
        assertEquals(123456789L, updated.date)
    }

    @Test
    fun `when repository throws exception, returns failure without mutating`() = runTest {
        fakeRepository.shouldThrow = true

        val result = useCase(
            id = 10L,
            amount = 200.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "فاتورة",
            date = 1000L
        )

        assertTrue(result.isFailure)
    }
}

private class FakeUpdateTransactionRepository : TransactionRepository {
    val transactions = mutableListOf<Transaction>()
    var shouldThrow = false

    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(transactions)
    override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(transactions.find { it.id == id })
    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(transactions.take(limit))
    override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override fun getTotalSumByType(type: TransactionType): Flow<Double> = flowOf(0.0)
    override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(0)

    override suspend fun insertTransaction(transaction: Transaction): Long {
        transactions.add(transaction)
        return transaction.id
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        if (shouldThrow) throw RuntimeException("Database error")
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
        } else {
            transactions.add(transaction)
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.removeIf { it.id == transaction.id }
    }

    override suspend fun deleteTransactionById(id: Long) {
        transactions.removeIf { it.id == id }
    }

    override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(emptyList())
    override suspend fun getCategoryById(id: Long): Category? = null
    override suspend fun insertCategory(category: Category): Long = 1L
    override suspend fun updateCategory(category: Category) {}
    override suspend fun deleteCategory(id: Long) {}
}

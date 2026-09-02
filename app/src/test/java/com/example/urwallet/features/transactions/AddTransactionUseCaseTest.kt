package com.example.urwallet.features.transactions

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import com.example.urwallet.features.transactions.domain.usecase.AddTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddTransactionUseCaseTest {

    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var useCase: AddTransactionUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeTransactionRepository()
        useCase = AddTransactionUseCase(fakeRepository)
    }

    @Test
    fun `when amount is zero or negative, returns failure`() = runTest {
        val resultZero = useCase(
            amount = 0.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "قهوة"
        )
        assertTrue(resultZero.isFailure)

        val resultNegative = useCase(
            amount = -50.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "قهوة"
        )
        assertTrue(resultNegative.isFailure)
    }

    @Test
    fun `when title is blank, returns failure`() = runTest {
        val resultBlank = useCase(
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "   "
        )
        assertTrue(resultBlank.isFailure)
    }

    @Test
    fun `when categoryId is zero or invalid, returns failure`() = runTest {
        val resultInvalidCategory = useCase(
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = 0L,
            title = "غداء"
        )
        assertTrue(resultInvalidCategory.isFailure)
    }

    @Test
    fun `when input is valid, inserts transaction and returns success`() = runTest {
        val result = useCase(
            amount = 250.0,
            type = TransactionType.EXPENSE,
            categoryId = 2L,
            title = "مشتريات بقالة",
            note = "سوبرماركت"
        )

        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        assertEquals(1, fakeRepository.insertedTransactions.size)
        assertEquals("مشتريات بقالة", fakeRepository.insertedTransactions.first().title)
        assertEquals(250.0, fakeRepository.insertedTransactions.first().amount, 0.001)
    }
}

class FakeTransactionRepository : TransactionRepository {
    val insertedTransactions = mutableListOf<Transaction>()

    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(insertedTransactions)
    override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(insertedTransactions.find { it.id == id })
    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(insertedTransactions.take(limit))
    override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override fun getTotalSumByType(type: TransactionType): Flow<Double> = flowOf(0.0)
    override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(0)

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val id = (insertedTransactions.size + 1).toLong()
        insertedTransactions.add(transaction.copy(id = id))
        return id
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val index = insertedTransactions.indexOfFirst { it.id == transaction.id }
        if (index >= 0) insertedTransactions[index] = transaction
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        insertedTransactions.removeAll { it.id == transaction.id }
    }

    override suspend fun deleteTransactionById(id: Long) {
        insertedTransactions.removeAll { it.id == id }
    }

    override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(emptyList())
    override suspend fun getCategoryById(id: Long): Category? = null
    override suspend fun insertCategory(category: Category): Long = 1L
    override suspend fun updateCategory(category: Category) {}
    override suspend fun deleteCategory(id: Long) {}
}

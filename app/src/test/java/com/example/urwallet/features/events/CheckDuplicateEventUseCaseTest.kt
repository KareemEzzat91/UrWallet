package com.example.urwallet.features.events

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.model.EventConfidence
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.FinancialEventSource
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.usecase.CheckDuplicateEventUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class CheckDuplicateEventUseCaseTest {

    private lateinit var fakeEventRepo: FakeFinancialEventRepository
    private lateinit var fakeTxRepo: TestTransactionRepo
    private lateinit var useCase: CheckDuplicateEventUseCase

    @Before
    fun setUp() {
        fakeEventRepo = FakeFinancialEventRepository()
        fakeTxRepo = TestTransactionRepo()
        useCase = CheckDuplicateEventUseCase(fakeEventRepo, fakeTxRepo)
    }

    @Test
    fun `when sourceIdentifier already in inbox, returns EXACT_MATCH`() = runTest {
        val existingEvent = FinancialEvent(
            id = 1L,
            amount = 500.0,
            type = TransactionType.EXPENSE,
            date = 1727000000000L,
            sourceType = FinancialEventSource.SMS,
            sourceIdentifier = "exact_source_hash_123",
            sender = "InstaPay",
            confidence = EventConfidence.HIGH,
            status = InboxStatus.PENDING,
            matchStatus = DuplicateMatchStatus.NEW_EVENT
        )
        fakeEventRepo.insertEvent(existingEvent)

        val result = useCase(
            amount = 500.0,
            type = TransactionType.EXPENSE,
            date = 1727000000000L,
            sourceIdentifier = "exact_source_hash_123"
        )

        assertEquals(DuplicateMatchStatus.EXACT_MATCH, result.status)
    }

    @Test
    fun `when transaction with same amount and type exists within 36h, returns POSSIBLE_MATCH`() = runTest {
        val eventTime = 1727000000000L
        val txTime = eventTime - TimeUnit.HOURS.toMillis(5) // within 36h

        val existingTx = Transaction(
            id = 99L,
            amount = 350.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "غداء كشري",
            date = txTime
        )
        fakeTxRepo.transactions.add(existingTx)

        val result = useCase(
            amount = 350.0,
            type = TransactionType.EXPENSE,
            date = eventTime,
            sourceIdentifier = "unique_source_999"
        )

        assertEquals(DuplicateMatchStatus.POSSIBLE_MATCH, result.status)
        assertEquals(99L, result.matchedTransactionId)
    }

    @Test
    fun `when no match exists, returns NEW_EVENT`() = runTest {
        val result = useCase(
            amount = 999.0,
            type = TransactionType.EXPENSE,
            date = 1727000000000L,
            sourceIdentifier = "brand_new_event_1"
        )

        assertEquals(DuplicateMatchStatus.NEW_EVENT, result.status)
    }

    private class TestTransactionRepo : TransactionRepository {
        val transactions = mutableListOf<Transaction>()

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(transactions)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(transactions.find { it.id == id })
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(transactions.take(limit))
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> {
            return flowOf(transactions.filter { it.date in startDate..endDate })
        }
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

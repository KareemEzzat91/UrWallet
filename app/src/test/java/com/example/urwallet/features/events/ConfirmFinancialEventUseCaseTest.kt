package com.example.urwallet.features.events

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyType
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.model.EventConfidence
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.FinancialEventSource
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.usecase.ConfirmFinancialEventUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import com.example.urwallet.features.transactions.domain.usecase.AddTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfirmFinancialEventUseCaseTest {

    private lateinit var fakeEventRepo: FakeFinancialEventRepository
    private lateinit var fakeTxRepo: TestTxRepo
    private lateinit var addTxUseCase: AddTransactionUseCase
    private lateinit var confirmUseCase: ConfirmFinancialEventUseCase

    @Before
    fun setUp() {
        fakeEventRepo = FakeFinancialEventRepository()
        fakeTxRepo = TestTxRepo()
        addTxUseCase = AddTransactionUseCase(fakeTxRepo)
        confirmUseCase = ConfirmFinancialEventUseCase(fakeEventRepo)
    }

    @Test
    fun `confirming event creates real transaction and scrubs rawMessage for privacy`() = runTest {
        val initialEvent = FinancialEvent(
            id = 10L,
            amount = 450.0,
            type = TransactionType.EXPENSE,
            date = 1727000000000L,
            sourceType = FinancialEventSource.SMS,
            sourceIdentifier = "sms_hash_123",
            rawMessage = "Sensitive SMS content with bank balance and account details",
            sender = "CIB",
            confidence = EventConfidence.HIGH,
            status = InboxStatus.PENDING,
            matchStatus = DuplicateMatchStatus.NEW_EVENT
        )
        fakeEventRepo.insertEvent(initialEvent)

        val result = confirmUseCase(
            eventId = 10L,
            categoryId = 1L,
            title = "سوبرماركت كارفور",
            note = "مشتريات أسبوعية",
            counterparty = Counterparty(name = "كارفور", type = CounterpartyType.MERCHANT)
        )

        assertTrue(result.isSuccess)
        val createdTxId = result.getOrNull()
        assertNotNull(createdTxId)

        // Event in inbox is updated to CONFIRMED
        val updatedEvent = fakeEventRepo.getEventById(10L)
        assertNotNull(updatedEvent)
        assertEquals(InboxStatus.CONFIRMED, updatedEvent!!.status)
        assertEquals(createdTxId, updatedEvent.matchedTransactionId)

        // Privacy refinement: rawMessage MUST be scrubbed (null)
        assertNull("Raw SMS must be scrubbed after confirmation", updatedEvent.rawMessage)
    }

    @Test
    fun `confirming event saves counterparty phone mapping when requested`() = runTest {
        val initialEvent = FinancialEvent(
            id = 20L,
            amount = 1200.0,
            type = TransactionType.EXPENSE,
            date = 1727000000000L,
            sourceType = FinancialEventSource.SMS,
            sourceIdentifier = "sms_hash_456",
            sender = "InstaPay",
            confidence = EventConfidence.HIGH,
            status = InboxStatus.PENDING,
            matchStatus = DuplicateMatchStatus.NEW_EVENT
        )
        fakeEventRepo.insertEvent(initialEvent)

        val counterparty = Counterparty(
            name = "أحمد مصطفى",
            phoneNumber = "01012345678",
            type = CounterpartyType.PERSON
        )

        val result = confirmUseCase(
            eventId = 20L,
            categoryId = 2L,
            title = "تحويل لأحمد",
            counterparty = counterparty,
            saveCounterpartyMapping = true
        )

        assertTrue(result.isSuccess)
        val savedMapping = fakeEventRepo.getMappingForPhone("01012345678")
        assertNotNull(savedMapping)
        assertEquals("أحمد مصطفى", savedMapping!!.name)
    }

    @Test
    fun `confirming already confirmed event is idempotent and returns existing transaction id`() = runTest {
        val confirmedEvent = FinancialEvent(
            id = 30L,
            amount = 300.0,
            type = TransactionType.EXPENSE,
            date = 1727000000000L,
            sourceType = FinancialEventSource.SMS,
            sourceIdentifier = "sms_hash_30",
            sender = "VodafoneCash",
            confidence = EventConfidence.HIGH,
            status = InboxStatus.CONFIRMED,
            matchedTransactionId = 555L,
            matchStatus = DuplicateMatchStatus.EXACT_MATCH
        )
        fakeEventRepo.insertEvent(confirmedEvent)

        val result = confirmUseCase(
            eventId = 30L,
            categoryId = 1L,
            title = "Already confirmed"
        )

        assertTrue(result.isSuccess)
        assertEquals(555L, result.getOrNull())
    }

    @Test
    fun `confirming dismissed event returns failure`() = runTest {
        val dismissedEvent = FinancialEvent(
            id = 40L,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            date = 1727000000000L,
            sourceType = FinancialEventSource.SMS,
            sourceIdentifier = "sms_hash_40",
            sender = "Promo",
            confidence = EventConfidence.LOW,
            status = InboxStatus.DISMISSED,
            matchStatus = DuplicateMatchStatus.NEW_EVENT
        )
        fakeEventRepo.insertEvent(dismissedEvent)

        val result = confirmUseCase(
            eventId = 40L,
            categoryId = 1L,
            title = "Dismissed event"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    private class TestTxRepo : TransactionRepository {
        val inserted = mutableListOf<Transaction>()

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(inserted)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(inserted.find { it.id == id })
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(inserted)
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTotalSumByType(type: TransactionType): Flow<Double> = flowOf(0.0)
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(0)

        override suspend fun insertTransaction(transaction: Transaction): Long {
            val id = (inserted.size + 1).toLong()
            inserted.add(transaction.copy(id = id))
            return id
        }
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

package com.example.urwallet.features.events

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.data.parser.ExtractorRegistry
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyType
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.repository.ContactResolutionRepository
import com.example.urwallet.features.events.domain.usecase.CheckDuplicateEventUseCase
import com.example.urwallet.features.events.domain.usecase.ProcessIncomingSmsUseCase
import com.example.urwallet.features.events.domain.usecase.SuggestCategoryUseCase
import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import com.example.urwallet.features.people.domain.usecase.ResolvePersonForCounterpartyUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ProcessIncomingSmsUseCaseTest {

    private lateinit var fakeEventRepo: FakeFinancialEventRepository
    private lateinit var fakeTxRepo: TestTransactionRepo
    private lateinit var fakePeopleRepo: TestPeopleRepo
    private lateinit var fakeCategoryRepo: TestCategoryRepo
    private lateinit var fakeContactRepo: TestContactResolutionRepo
    private lateinit var useCase: ProcessIncomingSmsUseCase

    @Before
    fun setUp() {
        fakeEventRepo = FakeFinancialEventRepository()
        fakeTxRepo = TestTransactionRepo()
        fakePeopleRepo = TestPeopleRepo()
        fakeCategoryRepo = TestCategoryRepo()
        fakeContactRepo = TestContactResolutionRepo()

        val checkDuplicateUseCase = CheckDuplicateEventUseCase(fakeEventRepo, fakeTxRepo)
        val resolvePersonUseCase = ResolvePersonForCounterpartyUseCase(fakePeopleRepo, fakeEventRepo)
        val suggestCategoryUseCase = SuggestCategoryUseCase(fakeEventRepo, fakeCategoryRepo)

        useCase = ProcessIncomingSmsUseCase(
            extractorRegistry = ExtractorRegistry(),
            financialEventRepository = fakeEventRepo,
            contactResolutionRepository = fakeContactRepo,
            checkDuplicateEventUseCase = checkDuplicateUseCase,
            resolvePersonForCounterpartyUseCase = resolvePersonUseCase,
            suggestCategoryUseCase = suggestCategoryUseCase
        )
    }

    @Test
    fun `when financial SMS received, extracts and returns event with positive id`() = runTest {
        val message = "تم تحويل 500.00 ج.م إلى 01012345678 بنجاح"
        val sender = "InstaPay"
        val timestamp = 1727000000000L

        val event = useCase(sender = sender, message = message, timestamp = timestamp)

        assertNotNull(event)
        assertEquals(500.0, event!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, event.type)
        assertEquals(1L, event.id)
    }

    @Test
    fun `when same SMS processed again, returns null due to sourceIdentifier idempotency`() = runTest {
        val message = "تم تحويل 250.00 ج.م إلى 01099887766 بنجاح"
        val sender = "InstaPay"
        val timestamp = 1727000000000L

        val first = useCase(sender = sender, message = message, timestamp = timestamp)
        assertNotNull(first)

        val duplicate = useCase(sender = sender, message = message, timestamp = timestamp)
        assertNull("Duplicate SMS by sourceIdentifier must be rejected", duplicate)
    }

    @Test
    fun `when insertEvent returns -1 on conflict, returns null to avoid fake events and false badges`() = runTest {
        val conflictRepo = object : FakeFinancialEventRepository() {
            override suspend fun insertEvent(event: FinancialEvent): Long = -1L
        }
        val conflictUseCase = ProcessIncomingSmsUseCase(
            extractorRegistry = ExtractorRegistry(),
            financialEventRepository = conflictRepo,
            contactResolutionRepository = fakeContactRepo,
            checkDuplicateEventUseCase = CheckDuplicateEventUseCase(conflictRepo, fakeTxRepo),
            resolvePersonForCounterpartyUseCase = ResolvePersonForCounterpartyUseCase(fakePeopleRepo, conflictRepo),
            suggestCategoryUseCase = SuggestCategoryUseCase(conflictRepo, fakeCategoryRepo)
        )

        val message = "تم تحويل 100.00 ج.م إلى 01055443322 بنجاح"
        val sender = "InstaPay"
        val timestamp = 1727000000000L

        val result = conflictUseCase(sender = sender, message = message, timestamp = timestamp)
        assertNull("Must return null when insertEvent fails with conflict code <= 0", result)
    }

    @Test
    fun `when OTP or non financial SMS received, returns null`() = runTest {
        val otpMessage = "Your OTP verification code is 492812. Do not share it with anyone."
        val sender = "CIB"
        val timestamp = System.currentTimeMillis()

        val result = useCase(sender = sender, message = otpMessage, timestamp = timestamp)
        assertNull(result)
    }

    private class TestContactResolutionRepo : ContactResolutionRepository {
        override suspend fun resolveContactName(phoneNumber: String): String? = null
        override fun hasContactsPermission(): Boolean = false
    }

    private class TestPeopleRepo : PeopleRepository {
        override fun getAllPeople(): Flow<List<Person>> = flowOf(emptyList())
        override fun getPersonById(id: Long): Flow<Person?> = flowOf(null)
        override suspend fun getPersonByIdSync(id: Long): Person? = null
        override suspend fun getPersonByName(name: String): Person? = null
        override suspend fun getPersonByPhone(phoneNumber: String): Person? = null
        override suspend fun insertPerson(person: Person): Long = 1L
        override suspend fun updatePerson(person: Person) {}
        override suspend fun deletePerson(id: Long) {}
        override fun searchPeople(query: String): Flow<List<Person>> = flowOf(emptyList())
        override fun getObligationsByPerson(personId: Long): Flow<List<com.example.urwallet.features.people.domain.model.FinancialObligation>> = flowOf(emptyList())
        override suspend fun getActiveObligationsByPersonSync(personId: Long): List<com.example.urwallet.features.people.domain.model.FinancialObligation> = emptyList()
        override fun getActiveObligations(): Flow<List<com.example.urwallet.features.people.domain.model.FinancialObligation>> = flowOf(emptyList())
        override fun getAllObligations(): Flow<List<com.example.urwallet.features.people.domain.model.FinancialObligation>> = flowOf(emptyList())
        override fun getObligationById(id: Long): Flow<com.example.urwallet.features.people.domain.model.FinancialObligation?> = flowOf(null)
        override suspend fun getObligationByIdSync(id: Long): com.example.urwallet.features.people.domain.model.FinancialObligation? = null
        override suspend fun insertObligation(obligation: com.example.urwallet.features.people.domain.model.FinancialObligation): Long = 1L
        override suspend fun updateObligation(obligation: com.example.urwallet.features.people.domain.model.FinancialObligation) {}
        override suspend fun deleteObligation(id: Long) {}
        override suspend fun recordSettlementAtomic(settlement: com.example.urwallet.features.people.domain.model.ObligationSettlement): com.example.urwallet.features.people.domain.model.FinancialObligation {
            throw UnsupportedOperationException()
        }
        override fun getSettlementsForObligation(obligationId: Long): Flow<List<com.example.urwallet.features.people.domain.model.ObligationSettlement>> = flowOf(emptyList())
        override suspend fun clearPersonFromTransactions(personId: Long) {}
    }

    private class TestCategoryRepo : CategoryRepository {
        override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: Long): Category? = null
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    private class TestTransactionRepo : TransactionRepository {
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

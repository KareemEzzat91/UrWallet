package com.example.urwallet.features.events

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.usecase.SuggestObligationSettlementUseCase
import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationSettlement
import com.example.urwallet.features.people.domain.model.ObligationStatus
import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SuggestObligationSettlementUseCaseTest {

    private lateinit var fakePeopleRepo: FakePeopleRepository
    private lateinit var suggestObligationSettlementUseCase: SuggestObligationSettlementUseCase

    @Before
    fun setUp() {
        fakePeopleRepo = FakePeopleRepository()
        suggestObligationSettlementUseCase = SuggestObligationSettlementUseCase(fakePeopleRepo)
    }

    @Test
    fun `INCOME event suggests settlement for OWED_TO_ME obligation`() = runTest {
        val personId = 1L
        val obligation = FinancialObligation(
            id = 101L,
            personId = personId,
            amount = 500.0,
            direction = ObligationDirection.OWED_TO_ME,
            status = ObligationStatus.OPEN,
            remainingAmount = 500.0,
            reason = "سلفة غداء"
        )
        fakePeopleRepo.obligations.add(obligation)

        val suggestions = suggestObligationSettlementUseCase(
            personId = personId,
            amount = 500.0,
            type = TransactionType.INCOME
        )

        assertEquals(1, suggestions.size)
        val first = suggestions.first()
        assertEquals(101L, first.obligation.id)
        assertTrue(first.isExactAmountMatch)
        assertEquals(500.0, first.suggestedAmount, 0.001)
    }

    @Test
    fun `EXPENSE event suggests settlement for I_OWE obligation`() = runTest {
        val personId = 2L
        val obligation = FinancialObligation(
            id = 102L,
            personId = personId,
            amount = 1000.0,
            direction = ObligationDirection.I_OWE,
            status = ObligationStatus.OPEN,
            remainingAmount = 1000.0,
            reason = "قسط جمعية"
        )
        fakePeopleRepo.obligations.add(obligation)

        val suggestions = suggestObligationSettlementUseCase(
            personId = personId,
            amount = 300.0,
            type = TransactionType.EXPENSE
        )

        assertEquals(1, suggestions.size)
        val first = suggestions.first()
        assertEquals(102L, first.obligation.id)
        assertFalse(first.isExactAmountMatch)
        assertEquals(300.0, first.suggestedAmount, 0.001)
    }

    @Test
    fun `direction mismatch produces zero suggestions`() = runTest {
        val personId = 1L
        // Person owes me money, but this event is EXPENSE (I spent money)
        val obligation = FinancialObligation(
            id = 103L,
            personId = personId,
            amount = 500.0,
            direction = ObligationDirection.OWED_TO_ME,
            status = ObligationStatus.OPEN,
            remainingAmount = 500.0,
            reason = "سلفة"
        )
        fakePeopleRepo.obligations.add(obligation)

        val suggestions = suggestObligationSettlementUseCase(
            personId = personId,
            amount = 500.0,
            type = TransactionType.EXPENSE // Mismatch: EXPENSE does not settle money owed to me
        )

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `already settled obligations are excluded`() = runTest {
        val personId = 1L
        val settledObligation = FinancialObligation(
            id = 104L,
            personId = personId,
            amount = 400.0,
            direction = ObligationDirection.OWED_TO_ME,
            status = ObligationStatus.SETTLED,
            remainingAmount = 0.0,
            settledAmount = 400.0,
            reason = "دين سابق مسدد"
        )
        fakePeopleRepo.obligations.add(settledObligation)

        val suggestions = suggestObligationSettlementUseCase(
            personId = personId,
            amount = 400.0,
            type = TransactionType.INCOME
        )

        assertTrue(suggestions.isEmpty())
    }

    private class FakePeopleRepository : PeopleRepository {
        val obligations = mutableListOf<FinancialObligation>()

        override fun getAllPeople(): Flow<List<Person>> = flowOf(emptyList())
        override fun getPersonById(id: Long): Flow<Person?> = flowOf(null)
        override suspend fun getPersonByIdSync(id: Long): Person? = null
        override suspend fun getPersonByPhone(phoneNumber: String): Person? = null
        override suspend fun getPersonByName(name: String): Person? = null
        override fun searchPeople(query: String): Flow<List<Person>> = flowOf(emptyList())
        override suspend fun insertPerson(person: Person): Long = person.id
        override suspend fun updatePerson(person: Person) {}
        override suspend fun deletePerson(id: Long) {}

        override fun getObligationsByPerson(personId: Long): Flow<List<FinancialObligation>> =
            flowOf(obligations.filter { it.personId == personId })
        override suspend fun getActiveObligationsByPersonSync(personId: Long): List<FinancialObligation> =
            obligations.filter { it.personId == personId }
        override fun getActiveObligations(): Flow<List<FinancialObligation>> = flowOf(obligations)
        override fun getAllObligations(): Flow<List<FinancialObligation>> = flowOf(obligations)
        override fun getObligationById(id: Long): Flow<FinancialObligation?> =
            flowOf(obligations.find { it.id == id })
        override suspend fun getObligationByIdSync(id: Long): FinancialObligation? =
            obligations.find { it.id == id }
        override suspend fun insertObligation(obligation: FinancialObligation): Long = obligation.id
        override suspend fun updateObligation(obligation: FinancialObligation) {}
        override suspend fun deleteObligation(id: Long) {}

        override suspend fun recordSettlementAtomic(settlement: ObligationSettlement): FinancialObligation {
            val ob = obligations.find { it.id == settlement.obligationId } ?: throw IllegalArgumentException()
            return ob
        }
        override fun getSettlementsForObligation(obligationId: Long): Flow<List<ObligationSettlement>> = flowOf(emptyList())
        override suspend fun clearPersonFromTransactions(personId: Long) {}
    }
}

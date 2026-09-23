package com.example.urwallet.features.people

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationSettlement
import com.example.urwallet.features.people.domain.model.ObligationStatus
import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import com.example.urwallet.features.people.domain.usecase.CreateFinancialObligationUseCase
import com.example.urwallet.features.people.domain.usecase.SettleObligationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinancialObligationUseCasesTest {

    private lateinit var peopleRepository: FakePeopleRepo
    private lateinit var createObligationUseCase: CreateFinancialObligationUseCase
    private lateinit var settleObligationUseCase: SettleObligationUseCase

    @Before
    fun setUp() {
        peopleRepository = FakePeopleRepo()
        peopleRepository.people.add(Person(id = 1L, name = "أحمد"))

        createObligationUseCase = CreateFinancialObligationUseCase(peopleRepository)
        settleObligationUseCase = SettleObligationUseCase(peopleRepository)
    }

    @Test
    fun `createObligation with invalid amount returns failure`() = runTest {
        val result = createObligationUseCase(
            personId = 1L,
            amount = -50.0,
            direction = ObligationDirection.OWED_TO_ME,
            reason = "سلفة"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `createObligation sets initial settledAmount to 0 and status to OPEN`() = runTest {
        val result = createObligationUseCase(
            personId = 1L,
            amount = 350.0,
            direction = ObligationDirection.I_OWE,
            reason = "غداء العمل"
        )

        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        val created = peopleRepository.getObligationByIdSync(id)!!
        assertEquals(350.0, created.amount, 0.001)
        assertEquals(0.0, created.settledAmount, 0.001)
        assertEquals(350.0, created.remainingAmount, 0.001)
        assertEquals(ObligationStatus.OPEN, created.status)
        assertEquals(ObligationDirection.I_OWE, created.direction)
        assertEquals("غداء العمل", created.reason)
    }

    @Test
    fun `settleObligation with amount exceeding remaining returns failure`() = runTest {
        val ob = FinancialObligation(
            id = 10L,
            personId = 1L,
            reason = "سلفة",
            amount = 1000.0,
            settledAmount = 400.0,
            remainingAmount = 600.0,
            direction = ObligationDirection.OWED_TO_ME,
            status = ObligationStatus.PARTIALLY_SETTLED
        )
        peopleRepository.obligations.add(ob)

        val result = settleObligationUseCase(
            obligationId = 10L,
            amount = 700.0 // Exceeds 600
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `settleObligation partial update updates settled amount and status`() = runTest {
        val ob = FinancialObligation(
            id = 11L,
            personId = 1L,
            reason = "قسط",
            amount = 1000.0,
            settledAmount = 0.0,
            remainingAmount = 1000.0,
            direction = ObligationDirection.OWED_TO_ME,
            status = ObligationStatus.OPEN
        )
        peopleRepository.obligations.add(ob)

        val result = settleObligationUseCase(
            obligationId = 11L,
            amount = 400.0
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(400.0, updated.settledAmount, 0.001)
        assertEquals(600.0, updated.remainingAmount, 0.001)
        assertEquals(ObligationStatus.PARTIALLY_SETTLED, updated.status)
    }

    @Test
    fun `settleObligation full settlement marks status as SETTLED`() = runTest {
        val ob = FinancialObligation(
            id = 12L,
            personId = 1L,
            reason = "قسط أخير",
            amount = 500.0,
            settledAmount = 200.0,
            remainingAmount = 300.0,
            direction = ObligationDirection.OWED_TO_ME,
            status = ObligationStatus.PARTIALLY_SETTLED
        )
        peopleRepository.obligations.add(ob)

        val result = settleObligationUseCase(
            obligationId = 12L,
            amount = 300.0
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(500.0, updated.settledAmount, 0.001)
        assertEquals(0.0, updated.remainingAmount, 0.001)
        assertEquals(ObligationStatus.SETTLED, updated.status)
    }

    // --- Fake ---
    class FakePeopleRepo : PeopleRepository {
        val people = mutableListOf<Person>()
        val obligations = mutableListOf<FinancialObligation>()
        val settlements = mutableListOf<ObligationSettlement>()
        private var nextObId = 1L
        private var nextSetId = 1L

        override suspend fun insertPerson(person: Person): Long = 1L
        override suspend fun updatePerson(person: Person) {}
        override suspend fun deletePerson(id: Long) {}
        override fun getAllPeople(): Flow<List<Person>> = flowOf(emptyList())
        override fun getPersonById(id: Long): Flow<Person?> = flowOf(people.find { it.id == id })
        override suspend fun getPersonByIdSync(id: Long): Person? = people.find { it.id == id }
        override suspend fun getPersonByPhone(phoneNumber: String): Person? = null
        override suspend fun getPersonByName(name: String): Person? = null
        override fun searchPeople(query: String): Flow<List<Person>> = flowOf(emptyList())

        override suspend fun insertObligation(obligation: FinancialObligation): Long {
            val id = if (obligation.id == 0L) nextObId++ else obligation.id
            val o = obligation.copy(id = id)
            obligations.add(o)
            return id
        }

        override suspend fun updateObligation(obligation: FinancialObligation) {
            val idx = obligations.indexOfFirst { it.id == obligation.id }
            if (idx != -1) obligations[idx] = obligation
        }

        override suspend fun deleteObligation(id: Long) {
            obligations.removeAll { it.id == id }
        }

        override fun getObligationById(id: Long): Flow<FinancialObligation?> =
            flowOf(obligations.find { it.id == id })

        override suspend fun getObligationByIdSync(id: Long): FinancialObligation? =
            obligations.find { it.id == id }

        override fun getObligationsByPerson(personId: Long): Flow<List<FinancialObligation>> =
            flowOf(obligations.filter { it.personId == personId })

        override suspend fun getActiveObligationsByPersonSync(personId: Long): List<FinancialObligation> =
            obligations.filter { it.personId == personId && it.status != ObligationStatus.SETTLED }

        override fun getActiveObligations(): Flow<List<FinancialObligation>> =
            flowOf(obligations.filter { it.status != ObligationStatus.SETTLED })

        override fun getAllObligations(): Flow<List<FinancialObligation>> = flowOf(obligations)

        override suspend fun recordSettlementAtomic(settlement: ObligationSettlement): FinancialObligation {
            val ob = getObligationByIdSync(settlement.obligationId)
                ?: throw IllegalArgumentException("Obligation not found: ${settlement.obligationId}")
            require(settlement.amount > 0.0) { "Settlement amount must be positive" }
            require(settlement.amount <= ob.remainingAmount + 0.001) {
                "Settlement amount (${settlement.amount}) cannot exceed remaining amount (${ob.remainingAmount})"
            }

            val newSettled = (ob.settledAmount + settlement.amount).coerceIn(0.0, ob.amount)
            val newRemaining = (ob.amount - newSettled).coerceIn(0.0, ob.amount)
            val newStatus = when {
                newRemaining <= 0.0001 -> ObligationStatus.SETTLED
                newSettled > 0.0 -> ObligationStatus.PARTIALLY_SETTLED
                else -> ObligationStatus.OPEN
            }

            settlements.add(settlement.copy(id = nextSetId++))

            val updated = ob.copy(
                settledAmount = newSettled,
                remainingAmount = newRemaining,
                status = newStatus
            )
            updateObligation(updated)
            return updated
        }

        override fun getSettlementsForObligation(obligationId: Long): Flow<List<ObligationSettlement>> =
            flowOf(settlements.filter { it.obligationId == obligationId })

        override suspend fun clearPersonFromTransactions(personId: Long) {}
    }
}

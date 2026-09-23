package com.example.urwallet.features.people

import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationSettlement
import com.example.urwallet.features.people.domain.model.ObligationStatus
import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import com.example.urwallet.features.people.domain.usecase.CreatePersonUseCase
import com.example.urwallet.features.people.domain.usecase.DeletePersonUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PersonUseCasesTest {

    private lateinit var peopleRepository: FakePeopleRepository
    private lateinit var createPersonUseCase: CreatePersonUseCase
    private lateinit var deletePersonUseCase: DeletePersonUseCase

    @Before
    fun setUp() {
        peopleRepository = FakePeopleRepository()
        createPersonUseCase = CreatePersonUseCase(peopleRepository)
        deletePersonUseCase = DeletePersonUseCase(peopleRepository)
    }

    @Test
    fun `createPerson with blank name returns failure`() = runTest {
        val result = createPersonUseCase("   ", "01012345678", null)
        assertTrue(result.isFailure)
    }

    @Test
    fun `createPerson with valid name creates and returns person id`() = runTest {
        val result = createPersonUseCase("علي حسن", "01123456789", "ملاحظات")
        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        assertTrue(id > 0)
        val person = peopleRepository.getPersonByIdSync(id)
        assertEquals("علي حسن", person?.name)
        assertEquals("01123456789", person?.phoneNumber)
    }

    @Test
    fun `deletePerson with active obligations returns failure`() = runTest {
        val personId = createPersonUseCase("كريم", "01234567890", null).getOrThrow()

        // Add an active open obligation
        peopleRepository.addObligation(
            FinancialObligation(
                id = 101L,
                personId = personId,
                reason = "سلفة",
                amount = 500.0,
                remainingAmount = 500.0,
                direction = ObligationDirection.OWED_TO_ME,
                status = ObligationStatus.OPEN
            )
        )

        val result = deletePersonUseCase(personId)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `deletePerson with no active debts unlinks transactions and deletes person`() = runTest {
        val personId = createPersonUseCase("يوسف", null, null).getOrThrow()

        // Add a settled obligation
        peopleRepository.addObligation(
            FinancialObligation(
                id = 102L,
                personId = personId,
                reason = "دين مسدد",
                amount = 300.0,
                settledAmount = 300.0,
                remainingAmount = 0.0,
                direction = ObligationDirection.I_OWE,
                status = ObligationStatus.SETTLED
            )
        )

        val result = deletePersonUseCase(personId)
        assertTrue(result.isSuccess)

        assertEquals(personId, peopleRepository.deletedPersonId)
        val personAfter = peopleRepository.getPersonByIdSync(personId)
        assertEquals(null, personAfter)
    }

    // --- Fakes ---
    class FakePeopleRepository : PeopleRepository {
        val people = mutableListOf<Person>()
        val obligations = mutableListOf<FinancialObligation>()
        var deletedPersonId: Long? = null
        private var nextPersonId = 1L

        override suspend fun insertPerson(person: Person): Long {
            val id = if (person.id == 0L) nextPersonId++ else person.id
            val p = person.copy(id = id)
            people.add(p)
            return id
        }

        override suspend fun updatePerson(person: Person) {
            val idx = people.indexOfFirst { it.id == person.id }
            if (idx != -1) people[idx] = person
        }

        override suspend fun deletePerson(id: Long) {
            deletedPersonId = id
            people.removeAll { it.id == id }
            obligations.removeAll { it.personId == id }
        }

        override fun getAllPeople(): Flow<List<Person>> = flowOf(people)
        override fun getPersonById(id: Long): Flow<Person?> = flowOf(people.find { it.id == id })
        override suspend fun getPersonByIdSync(id: Long): Person? = people.find { it.id == id }
        override suspend fun getPersonByPhone(phoneNumber: String): Person? = people.find { it.phoneNumber == phoneNumber }
        override suspend fun getPersonByName(name: String): Person? = people.find { it.name.equals(name, ignoreCase = true) }
        override fun searchPeople(query: String): Flow<List<Person>> = flowOf(people.filter { it.name.contains(query) })

        fun addObligation(ob: FinancialObligation) {
            obligations.add(ob)
        }

        override suspend fun insertObligation(obligation: FinancialObligation): Long = 1L
        override suspend fun updateObligation(obligation: FinancialObligation) {}
        override suspend fun deleteObligation(id: Long) {}
        override fun getObligationById(id: Long): Flow<FinancialObligation?> = flowOf(obligations.find { it.id == id })
        override suspend fun getObligationByIdSync(id: Long): FinancialObligation? = obligations.find { it.id == id }
        override fun getObligationsByPerson(personId: Long): Flow<List<FinancialObligation>> =
            flowOf(obligations.filter { it.personId == personId })
        override suspend fun getActiveObligationsByPersonSync(personId: Long): List<FinancialObligation> =
            obligations.filter { it.personId == personId && it.status != ObligationStatus.SETTLED }
        override fun getActiveObligations(): Flow<List<FinancialObligation>> =
            flowOf(obligations.filter { it.status != ObligationStatus.SETTLED })
        override fun getAllObligations(): Flow<List<FinancialObligation>> = flowOf(obligations)

        override suspend fun recordSettlementAtomic(settlement: ObligationSettlement): FinancialObligation =
            obligations.first()

        override fun getSettlementsForObligation(obligationId: Long): Flow<List<ObligationSettlement>> = flowOf(emptyList())

        override suspend fun clearPersonFromTransactions(personId: Long) {}
    }
}

package com.example.urwallet.features.people.domain.repository

import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationSettlement
import com.example.urwallet.features.people.domain.model.Person
import kotlinx.coroutines.flow.Flow

interface PeopleRepository {

    // --- People ---
    fun getAllPeople(): Flow<List<Person>>
    fun getPersonById(id: Long): Flow<Person?>
    suspend fun getPersonByIdSync(id: Long): Person?
    suspend fun getPersonByPhone(phoneNumber: String): Person?
    suspend fun getPersonByName(name: String): Person?
    fun searchPeople(query: String): Flow<List<Person>>
    suspend fun insertPerson(person: Person): Long
    suspend fun updatePerson(person: Person)
    suspend fun deletePerson(id: Long)

    // --- Financial Obligations ---
    fun getObligationsByPerson(personId: Long): Flow<List<FinancialObligation>>
    suspend fun getActiveObligationsByPersonSync(personId: Long): List<FinancialObligation>
    fun getActiveObligations(): Flow<List<FinancialObligation>>
    fun getAllObligations(): Flow<List<FinancialObligation>>
    fun getObligationById(id: Long): Flow<FinancialObligation?>
    suspend fun getObligationByIdSync(id: Long): FinancialObligation?
    suspend fun insertObligation(obligation: FinancialObligation): Long
    suspend fun updateObligation(obligation: FinancialObligation)
    suspend fun deleteObligation(id: Long)

    // --- Settlements ---
    suspend fun recordSettlementAtomic(settlement: ObligationSettlement): FinancialObligation
    fun getSettlementsForObligation(obligationId: Long): Flow<List<ObligationSettlement>>

    // --- Transaction association ---
    suspend fun clearPersonFromTransactions(personId: Long)
}

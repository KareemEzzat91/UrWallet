package com.example.urwallet.features.people.data.repository

import com.example.urwallet.features.people.data.dao.FinancialObligationDao
import com.example.urwallet.features.people.data.dao.PersonDao
import com.example.urwallet.features.people.data.entity.FinancialObligationEntity
import com.example.urwallet.features.people.data.entity.ObligationSettlementEntity
import com.example.urwallet.features.people.data.entity.PersonEntity
import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationSettlement
import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import com.example.urwallet.features.transactions.data.dao.TransactionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import com.example.urwallet.core.database.UrWalletDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepositoryImpl @Inject constructor(
    private val urWalletDatabase: UrWalletDatabase,
    private val personDao: PersonDao,
    private val obligationDao: FinancialObligationDao,
    private val transactionDao: TransactionDao
) : PeopleRepository {

    // --- People ---

    override fun getAllPeople(): Flow<List<Person>> {
        return personDao.getAllPeople().map { list -> list.map { it.toDomain() } }
    }

    override fun getPersonById(id: Long): Flow<Person?> {
        return personDao.getPersonById(id).map { it?.toDomain() }
    }

    override suspend fun getPersonByIdSync(id: Long): Person? {
        return personDao.getPersonByIdSync(id)?.toDomain()
    }

    override suspend fun getPersonByPhone(phoneNumber: String): Person? {
        return personDao.getPersonByPhone(phoneNumber)?.toDomain()
    }

    override suspend fun getPersonByName(name: String): Person? {
        return personDao.getPersonByName(name)?.toDomain()
    }

    override fun searchPeople(query: String): Flow<List<Person>> {
        return personDao.searchPeople(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertPerson(person: Person): Long {
        return personDao.insertPerson(PersonEntity.fromDomain(person))
    }

    override suspend fun updatePerson(person: Person) {
        personDao.updatePerson(PersonEntity.fromDomain(person))
    }

    override suspend fun deletePerson(id: Long) {
        urWalletDatabase.withTransaction {
            // Clear transactions reference first to preserve financial ledger
            transactionDao.clearPersonFromTransactions(id)
            personDao.deletePersonById(id)
        }
    }

    // --- Financial Obligations ---

    override fun getObligationsByPerson(personId: Long): Flow<List<FinancialObligation>> {
        return obligationDao.getObligationsByPerson(personId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActiveObligationsByPersonSync(personId: Long): List<FinancialObligation> {
        return obligationDao.getActiveObligationsByPersonSync(personId).map { it.toDomain() }
    }

    override fun getActiveObligations(): Flow<List<FinancialObligation>> {
        return obligationDao.getActiveObligations().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllObligations(): Flow<List<FinancialObligation>> {
        return obligationDao.getAllObligations().map { list -> list.map { it.toDomain() } }
    }

    override fun getObligationById(id: Long): Flow<FinancialObligation?> {
        return obligationDao.getObligationById(id).map { it?.toDomain() }
    }

    override suspend fun getObligationByIdSync(id: Long): FinancialObligation? {
        return obligationDao.getObligationByIdSync(id)?.toDomain()
    }

    override suspend fun insertObligation(obligation: FinancialObligation): Long {
        return obligationDao.insertObligation(FinancialObligationEntity.fromDomain(obligation))
    }

    override suspend fun updateObligation(obligation: FinancialObligation) {
        obligationDao.updateObligation(FinancialObligationEntity.fromDomain(obligation))
    }

    override suspend fun deleteObligation(id: Long) {
        obligationDao.deleteObligationById(id)
    }

    // --- Settlements ---

    override suspend fun recordSettlementAtomic(settlement: ObligationSettlement): FinancialObligation {
        val updatedEntity = obligationDao.recordSettlementAtomic(ObligationSettlementEntity.fromDomain(settlement))
        return updatedEntity.toDomain()
    }

    override fun getSettlementsForObligation(obligationId: Long): Flow<List<ObligationSettlement>> {
        return obligationDao.getSettlementsForObligation(obligationId).map { list -> list.map { it.toDomain() } }
    }

    // --- Transaction association ---

    override suspend fun clearPersonFromTransactions(personId: Long) {
        transactionDao.clearPersonFromTransactions(personId)
    }
}

package com.example.urwallet.features.events.data.repository

import com.example.urwallet.features.events.data.dao.CounterpartyMappingDao
import com.example.urwallet.features.events.data.dao.FinancialInboxDao
import com.example.urwallet.features.events.data.entity.CounterpartyMappingEntity
import com.example.urwallet.features.events.data.entity.FinancialInboxEntity
import com.example.urwallet.features.events.domain.model.CounterpartyMapping
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancialEventRepositoryImpl @Inject constructor(
    private val financialInboxDao: FinancialInboxDao,
    private val counterpartyMappingDao: CounterpartyMappingDao
) : FinancialEventRepository {

    override fun getPendingEvents(): Flow<List<FinancialEvent>> {
        return financialInboxDao.getPendingEvents().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllEvents(): Flow<List<FinancialEvent>> {
        return financialInboxDao.getAllEvents().map { list -> list.map { it.toDomain() } }
    }

    override fun getEventsByStatus(status: InboxStatus): Flow<List<FinancialEvent>> {
        return financialInboxDao.getEventsByStatus(status.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getPendingCount(): Flow<Int> {
        return financialInboxDao.getPendingCount()
    }

    override suspend fun getEventById(id: Long): FinancialEvent? {
        return financialInboxDao.getEventById(id)?.toDomain()
    }

    override suspend fun getEventBySourceIdentifier(sourceIdentifier: String): FinancialEvent? {
        return financialInboxDao.getEventBySourceIdentifier(sourceIdentifier)?.toDomain()
    }

    override suspend fun insertEvent(event: FinancialEvent): Long {
        return financialInboxDao.insertEvent(FinancialInboxEntity.fromDomain(event))
    }

    override suspend fun updateEvent(event: FinancialEvent) {
        financialInboxDao.updateEvent(FinancialInboxEntity.fromDomain(event))
    }

    override suspend fun markConfirmed(id: Long, transactionId: Long) {
        // Refinement 3: Immediately scrub sensitive rawMessage when confirmed
        financialInboxDao.markConfirmedAndScrub(id, transactionId)
    }

    override suspend fun markDismissed(id: Long) {
        // Refinement 3: Immediately scrub sensitive rawMessage when dismissed
        financialInboxDao.markDismissedAndScrub(id)
    }

    override suspend fun deleteEventById(id: Long) {
        financialInboxDao.deleteEventById(id)
    }

    override suspend fun getMappingForPhone(phoneNumber: String): CounterpartyMapping? {
        return counterpartyMappingDao.getMappingByPhone(phoneNumber)?.toDomain()
    }

    override suspend fun saveMapping(mapping: CounterpartyMapping) {
        counterpartyMappingDao.insertOrUpdate(CounterpartyMappingEntity.fromDomain(mapping))
    }
}

package com.example.urwallet.features.events.domain.repository

import com.example.urwallet.features.events.domain.model.CounterpartyMapping
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxStatus
import kotlinx.coroutines.flow.Flow

interface FinancialEventRepository {
    fun getPendingEvents(): Flow<List<FinancialEvent>>
    fun getAllEvents(): Flow<List<FinancialEvent>>
    fun getEventsByStatus(status: InboxStatus): Flow<List<FinancialEvent>>
    fun getPendingCount(): Flow<Int>
    suspend fun getEventById(id: Long): FinancialEvent?
    suspend fun getEventBySourceIdentifier(sourceIdentifier: String): FinancialEvent?
    suspend fun insertEvent(event: FinancialEvent): Long
    suspend fun updateEvent(event: FinancialEvent)
    suspend fun markConfirmed(id: Long, transactionId: Long)
    suspend fun markDismissed(id: Long)
    suspend fun deleteEventById(id: Long)

    // Local Phone-to-Counterparty Mappings
    suspend fun getMappingForPhone(phoneNumber: String): CounterpartyMapping?
    suspend fun saveMapping(mapping: CounterpartyMapping)
}

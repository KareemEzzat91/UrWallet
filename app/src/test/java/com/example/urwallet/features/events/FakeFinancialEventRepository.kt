package com.example.urwallet.features.events

import com.example.urwallet.features.events.domain.model.CounterpartyMapping
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeFinancialEventRepository : FinancialEventRepository {

    val events = mutableListOf<FinancialEvent>()
    val mappings = mutableListOf<CounterpartyMapping>()
    var lastScrubbedId: Long? = null

    override fun getPendingEvents(): Flow<List<FinancialEvent>> {
        return flowOf(events.filter { it.status == InboxStatus.PENDING })
    }

    override fun getAllEvents(): Flow<List<FinancialEvent>> = flowOf(events)

    override fun getEventsByStatus(status: InboxStatus): Flow<List<FinancialEvent>> {
        return flowOf(events.filter { it.status == status })
    }

    override fun getPendingCount(): Flow<Int> {
        return flowOf(events.count { it.status == InboxStatus.PENDING })
    }

    override suspend fun getEventById(id: Long): FinancialEvent? {
        return events.find { it.id == id }
    }

    override suspend fun getEventBySourceIdentifier(sourceIdentifier: String): FinancialEvent? {
        return events.find { it.sourceIdentifier == sourceIdentifier }
    }

    override suspend fun insertEvent(event: FinancialEvent): Long {
        val id = if (event.id > 0) event.id else (events.size + 1).toLong()
        events.add(event.copy(id = id))
        return id
    }

    override suspend fun updateEvent(event: FinancialEvent) {
        val idx = events.indexOfFirst { it.id == event.id }
        if (idx >= 0) events[idx] = event
    }

    override suspend fun markConfirmed(id: Long, transactionId: Long) {
        lastScrubbedId = id
        val idx = events.indexOfFirst { it.id == id }
        if (idx >= 0) {
            // Privacy refinement: rawMessage scrubbed (set to null) on confirm
            events[idx] = events[idx].copy(
                status = InboxStatus.CONFIRMED,
                matchedTransactionId = transactionId,
                rawMessage = null
            )
        }
    }

    override suspend fun markDismissed(id: Long) {
        lastScrubbedId = id
        val idx = events.indexOfFirst { it.id == id }
        if (idx >= 0) {
            // Privacy refinement: rawMessage scrubbed (set to null) on dismiss
            events[idx] = events[idx].copy(
                status = InboxStatus.DISMISSED,
                rawMessage = null
            )
        }
    }

    override suspend fun deleteEventById(id: Long) {
        events.removeAll { it.id == id }
    }

    override suspend fun getMappingForPhone(phoneNumber: String): CounterpartyMapping? {
        return mappings.find { it.phoneNumber == phoneNumber }
    }

    override suspend fun saveMapping(mapping: CounterpartyMapping) {
        mappings.removeAll { it.phoneNumber == mapping.phoneNumber }
        mappings.add(mapping)
    }
}

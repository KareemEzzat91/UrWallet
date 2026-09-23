package com.example.urwallet.features.events.domain.repository

import com.example.urwallet.features.events.domain.model.CategoryMapping
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyMapping
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxFilter
import com.example.urwallet.features.events.domain.model.InboxStatus
import kotlinx.coroutines.flow.Flow

interface FinancialEventRepository {
    fun getPendingEvents(): Flow<List<FinancialEvent>>
    fun getAllEvents(): Flow<List<FinancialEvent>>
    fun getEventsByStatus(status: InboxStatus): Flow<List<FinancialEvent>>
    fun getFilteredEvents(filter: InboxFilter): Flow<List<FinancialEvent>>
    fun getPendingCount(): Flow<Int>
    suspend fun getEventById(id: Long): FinancialEvent?
    suspend fun getEventBySourceIdentifier(sourceIdentifier: String): FinancialEvent?
    suspend fun insertEvent(event: FinancialEvent): Long
    suspend fun updateEvent(event: FinancialEvent)
    suspend fun markConfirmed(id: Long, transactionId: Long)
    suspend fun markDismissed(id: Long)
    suspend fun markAsDifferentTransaction(id: Long)
    suspend fun markDismissedBulk(ids: List<Long>)
    suspend fun markPendingBulk(ids: List<Long>)
    suspend fun deleteEventById(id: Long)

    // Atomic Confirmation + Optional Settlement Transaction
    suspend fun confirmEventAtomic(
        eventId: Long,
        categoryId: Long,
        title: String,
        amount: Double,
        type: com.example.urwallet.core.common.TransactionType,
        date: Long,
        note: String?,
        personId: Long?,
        counterparty: Counterparty?,
        saveCounterpartyMapping: Boolean,
        learnedCategoryPattern: String?,
        settleObligationId: Long?
    ): Result<Long>

    // Local Phone-to-Counterparty Mappings
    suspend fun getMappingForPhone(phoneNumber: String): CounterpartyMapping?
    suspend fun saveMapping(mapping: CounterpartyMapping)

    // Learned Category Mappings
    suspend fun getCategoryMapping(pattern: String): CategoryMapping?
    suspend fun saveCategoryMapping(mapping: CategoryMapping)
    fun getAllCategoryMappings(): Flow<List<CategoryMapping>>
    suspend fun getAllCategoryMappingsSync(): List<CategoryMapping>
}

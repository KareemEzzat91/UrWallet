package com.example.urwallet.features.events

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.CategoryMapping
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyMapping
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxFilter
import com.example.urwallet.features.events.domain.model.InboxSortOrder
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.model.InboxTab
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

open class FakeFinancialEventRepository : FinancialEventRepository {

    val events = mutableListOf<FinancialEvent>()
    val mappings = mutableListOf<CounterpartyMapping>()
    val categoryMappings = mutableListOf<CategoryMapping>()
    var lastScrubbedId: Long? = null

    override fun getPendingEvents(): Flow<List<FinancialEvent>> {
        return flowOf(events.filter { it.status == InboxStatus.PENDING })
    }

    override fun getAllEvents(): Flow<List<FinancialEvent>> = flowOf(events)

    override fun getEventsByStatus(status: InboxStatus): Flow<List<FinancialEvent>> {
        return flowOf(events.filter { it.status == status })
    }

    override fun getFilteredEvents(filter: InboxFilter): Flow<List<FinancialEvent>> {
        var list = events.asSequence()

        list = when (filter.tab) {
            InboxTab.ALL -> list
            InboxTab.PENDING -> list.filter { it.status == InboxStatus.PENDING }
            InboxTab.CONFIRMED -> list.filter { it.status == InboxStatus.CONFIRMED }
            InboxTab.DISMISSED -> list.filter { it.status == InboxStatus.DISMISSED }
            InboxTab.POSSIBLE_DUPLICATE -> list.filter {
                it.status == InboxStatus.PENDING &&
                    (it.matchStatus == DuplicateMatchStatus.POSSIBLE_MATCH || it.matchStatus == DuplicateMatchStatus.EXACT_MATCH)
            }
        }

        if (filter.query.isNotBlank()) {
            val q = filter.query.trim()
            list = list.filter { ev ->
                ev.sender.contains(q, ignoreCase = true) ||
                    (ev.counterparty?.name?.contains(q, ignoreCase = true) == true) ||
                    (ev.counterparty?.phoneNumber?.contains(q, ignoreCase = true) == true) ||
                    (ev.accountOrCard?.contains(q, ignoreCase = true) == true) ||
                    ev.amount.toString().contains(q)
            }
        }

        val sorted = when (filter.sortOrder) {
            InboxSortOrder.NEWEST_FIRST -> list.sortedByDescending { it.date }
            InboxSortOrder.OLDEST_FIRST -> list.sortedBy { it.date }
            InboxSortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
            InboxSortOrder.LOWEST_AMOUNT -> list.sortedBy { it.amount }
        }.toList()

        return flowOf(sorted)
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
            events[idx] = events[idx].copy(
                status = InboxStatus.DISMISSED,
                rawMessage = null
            )
        }
    }

    override suspend fun markAsDifferentTransaction(id: Long) {
        val idx = events.indexOfFirst { it.id == id }
        if (idx >= 0) {
            events[idx] = events[idx].copy(
                matchStatus = DuplicateMatchStatus.NEW_EVENT,
                matchedTransactionId = null
            )
        }
    }

    override suspend fun markDismissedBulk(ids: List<Long>) {
        ids.forEach { markDismissed(it) }
    }

    override suspend fun markPendingBulk(ids: List<Long>) {
        ids.forEach { id ->
            val idx = events.indexOfFirst { it.id == id }
            if (idx >= 0) {
                events[idx] = events[idx].copy(status = InboxStatus.PENDING)
            }
        }
    }

    override suspend fun deleteEventById(id: Long) {
        events.removeAll { it.id == id }
    }

    override suspend fun confirmEventAtomic(
        eventId: Long,
        categoryId: Long,
        title: String,
        amount: Double,
        type: TransactionType,
        date: Long,
        note: String?,
        personId: Long?,
        counterparty: Counterparty?,
        saveCounterpartyMapping: Boolean,
        learnedCategoryPattern: String?,
        settleObligationId: Long?
    ): Result<Long> {
        val dummyTxId = 999L
        markConfirmed(eventId, dummyTxId)
        if (saveCounterpartyMapping && counterparty != null && !counterparty.phoneNumber.isNullOrBlank()) {
            saveMapping(
                CounterpartyMapping(
                    phoneNumber = counterparty.phoneNumber,
                    name = counterparty.name,
                    type = counterparty.type,
                    personId = personId
                )
            )
        }
        if (!learnedCategoryPattern.isNullOrBlank()) {
            saveCategoryMapping(
                CategoryMapping(
                    pattern = learnedCategoryPattern.lowercase(),
                    categoryId = categoryId,
                    usageCount = 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return Result.success(dummyTxId)
    }

    override suspend fun getMappingForPhone(phoneNumber: String): CounterpartyMapping? {
        return mappings.find { it.phoneNumber == phoneNumber }
    }

    override suspend fun saveMapping(mapping: CounterpartyMapping) {
        mappings.removeAll { it.phoneNumber == mapping.phoneNumber }
        mappings.add(mapping)
    }

    override suspend fun getCategoryMapping(pattern: String): CategoryMapping? {
        return categoryMappings.find { it.pattern == pattern.lowercase() }
    }

    override suspend fun saveCategoryMapping(mapping: CategoryMapping) {
        categoryMappings.removeAll { it.pattern == mapping.pattern.lowercase() }
        categoryMappings.add(mapping.copy(pattern = mapping.pattern.lowercase()))
    }

    override fun getAllCategoryMappings(): Flow<List<CategoryMapping>> = flowOf(categoryMappings)

    override suspend fun getAllCategoryMappingsSync(): List<CategoryMapping> = categoryMappings.toList()
}

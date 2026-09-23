package com.example.urwallet.features.events.data.repository

import androidx.room.withTransaction
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.core.database.UrWalletDatabase
import com.example.urwallet.features.events.data.dao.CategoryMappingDao
import com.example.urwallet.features.events.data.dao.CounterpartyMappingDao
import com.example.urwallet.features.events.data.dao.FinancialInboxDao
import com.example.urwallet.features.events.data.entity.CategoryMappingEntity
import com.example.urwallet.features.events.data.entity.CounterpartyMappingEntity
import com.example.urwallet.features.events.data.entity.FinancialInboxEntity
import com.example.urwallet.features.events.domain.model.CategoryMapping
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyMapping
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxFilter
import com.example.urwallet.features.events.domain.model.InboxSortOrder
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.model.InboxTab
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import com.example.urwallet.features.people.data.dao.FinancialObligationDao
import com.example.urwallet.features.people.data.entity.ObligationSettlementEntity
import com.example.urwallet.features.people.domain.model.ObligationStatus
import com.example.urwallet.features.transactions.data.dao.TransactionDao
import com.example.urwallet.features.transactions.data.entity.TransactionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancialEventRepositoryImpl @Inject constructor(
    private val urWalletDatabase: UrWalletDatabase,
    private val financialInboxDao: FinancialInboxDao,
    private val counterpartyMappingDao: CounterpartyMappingDao,
    private val categoryMappingDao: CategoryMappingDao,
    private val transactionDao: TransactionDao,
    private val financialObligationDao: FinancialObligationDao
) : FinancialEventRepository {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun getPendingEvents(): Flow<List<FinancialEvent>> {
        return financialInboxDao.getPendingEvents().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllEvents(): Flow<List<FinancialEvent>> {
        return financialInboxDao.getAllEvents().map { list -> list.map { it.toDomain() } }
    }

    override fun getEventsByStatus(status: InboxStatus): Flow<List<FinancialEvent>> {
        return financialInboxDao.getEventsByStatus(status.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getFilteredEvents(filter: InboxFilter): Flow<List<FinancialEvent>> {
        val statusParam: String? = when (filter.tab) {
            InboxTab.ALL -> null
            InboxTab.PENDING -> InboxStatus.PENDING.name
            InboxTab.POSSIBLE_DUPLICATE -> InboxStatus.PENDING.name
            InboxTab.CONFIRMED -> InboxStatus.CONFIRMED.name
            InboxTab.DISMISSED -> InboxStatus.DISMISSED.name
        }

        val isDuplicateOnly = if (filter.tab == InboxTab.POSSIBLE_DUPLICATE) 1 else 0

        val sortByStr = when (filter.sortOrder) {
            InboxSortOrder.NEWEST_FIRST -> "DATE_DESC"
            InboxSortOrder.OLDEST_FIRST -> "DATE_ASC"
            InboxSortOrder.HIGHEST_AMOUNT -> "AMOUNT_DESC"
            InboxSortOrder.LOWEST_AMOUNT -> "AMOUNT_ASC"
        }

        val queryParam = filter.query.trim().ifBlank { null }

        return financialInboxDao.getFilteredEvents(
            status = statusParam,
            isDuplicateOnly = isDuplicateOnly,
            confidence = filter.confidence?.name,
            sourceType = filter.sourceType?.name,
            startDate = filter.startDate,
            endDate = filter.endDate,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            query = queryParam,
            sortBy = sortByStr
        ).map { list -> list.map { it.toDomain() } }
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
        // Scrub sensitive rawMessage when confirmed
        financialInboxDao.markConfirmedAndScrub(id, transactionId)
    }

    override suspend fun markDismissed(id: Long) {
        // Scrub sensitive rawMessage when dismissed
        financialInboxDao.markDismissedAndScrub(id)
    }

    override suspend fun markAsDifferentTransaction(id: Long) {
        financialInboxDao.markAsDifferentTransaction(id)
    }

    override suspend fun markDismissedBulk(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            financialInboxDao.markDismissedBulk(ids)
        }
    }

    override suspend fun markPendingBulk(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            financialInboxDao.markPendingBulk(ids)
        }
    }

    override suspend fun deleteEventById(id: Long) {
        financialInboxDao.deleteEventById(id)
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
    ): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            urWalletDatabase.withTransaction {
                // 1. Insert Transaction into financial ledger
                val txEntity = TransactionEntity(
                    id = 0L,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    title = title.trim(),
                    note = note?.trim()?.ifBlank { null },
                    date = date,
                    personId = personId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val transactionId = transactionDao.insertTransaction(txEntity)

                // 2. If obligation settlement requested, apply atomic settlement
                if (settleObligationId != null && settleObligationId > 0L) {
                    val obEntity = financialObligationDao.getObligationByIdSync(settleObligationId)
                        ?: throw IllegalArgumentException("الالتزام المالي غير موجود للتسوية")

                    if (obEntity.remainingAmount <= 0.001 || obEntity.status == ObligationStatus.SETTLED) {
                        throw IllegalStateException("الالتزام المالي مسدد بالفعل")
                    }

                    val settleAmount = amount.coerceAtMost(obEntity.remainingAmount)
                    val settlementEntity = ObligationSettlementEntity(
                        id = 0L,
                        obligationId = settleObligationId,
                        amount = settleAmount,
                        date = date,
                        note = "تسوية عبر الوارد المالي: $title",
                        relatedTransactionId = transactionId,
                        createdAt = System.currentTimeMillis()
                    )
                    financialObligationDao.insertSettlement(settlementEntity)

                    val newSettled = (obEntity.settledAmount + settleAmount).coerceAtMost(obEntity.amount)
                    val newRemaining = (obEntity.amount - newSettled).coerceAtLeast(0.0)
                    val newStatus = if (newRemaining <= 0.001) ObligationStatus.SETTLED else ObligationStatus.PARTIALLY_SETTLED

                    val updatedOb = obEntity.copy(
                        settledAmount = newSettled,
                        remainingAmount = newRemaining,
                        status = newStatus,
                        updatedAt = System.currentTimeMillis()
                    )
                    financialObligationDao.updateObligation(updatedOb)
                }

                // 3. Persist phone -> counterparty mapping if requested
                val phone = counterparty?.phoneNumber
                if (saveCounterpartyMapping && !phone.isNullOrBlank() && counterparty.name.isNotBlank()) {
                    counterpartyMappingDao.insertOrUpdate(
                        CounterpartyMappingEntity(
                            phoneNumber = phone,
                            name = counterparty.name.trim(),
                            type = counterparty.type.name,
                            personId = personId,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                // 4. Persist merchant/counterparty -> preferred category mapping
                if (!learnedCategoryPattern.isNullOrBlank()) {
                    val cleanPattern = learnedCategoryPattern.trim().lowercase()
                    val existing = categoryMappingDao.getMapping(cleanPattern)
                    val usageCount = if (existing != null && existing.categoryId == categoryId) existing.usageCount + 1 else 1
                    categoryMappingDao.insertOrUpdate(
                        CategoryMappingEntity(
                            pattern = cleanPattern,
                            categoryId = categoryId,
                            usageCount = usageCount,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                // 5. Mark inbox event confirmed and scrub rawMessage
                financialInboxDao.markConfirmedAndScrub(eventId, transactionId)

                transactionId
            }
        }
    }

    override suspend fun getMappingForPhone(phoneNumber: String): CounterpartyMapping? {
        return counterpartyMappingDao.getMappingByPhone(phoneNumber)?.toDomain()
    }

    override suspend fun saveMapping(mapping: CounterpartyMapping) {
        counterpartyMappingDao.insertOrUpdate(CounterpartyMappingEntity.fromDomain(mapping))
    }

    override suspend fun getCategoryMapping(pattern: String): CategoryMapping? {
        return categoryMappingDao.getMapping(pattern.trim().lowercase())?.toDomain()
    }

    override suspend fun saveCategoryMapping(mapping: CategoryMapping) {
        val cleanPattern = mapping.pattern.trim().lowercase()
        val existing = categoryMappingDao.getMapping(cleanPattern)
        val usageCount = if (existing != null && existing.categoryId == mapping.categoryId) {
            existing.usageCount + 1
        } else {
            1
        }
        categoryMappingDao.insertOrUpdate(
            CategoryMappingEntity(
                pattern = cleanPattern,
                categoryId = mapping.categoryId,
                usageCount = usageCount,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override fun getAllCategoryMappings(): Flow<List<CategoryMapping>> {
        return categoryMappingDao.getAllMappings().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllCategoryMappingsSync(): List<CategoryMapping> {
        return categoryMappingDao.getAllMappingsSync().map { it.toDomain() }
    }
}

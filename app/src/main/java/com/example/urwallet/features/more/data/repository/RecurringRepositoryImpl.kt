package com.example.urwallet.features.more.data.repository

import com.example.urwallet.features.more.data.dao.RecurringTransactionDao
import com.example.urwallet.features.more.data.entity.RecurringTransactionEntity
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringRepositoryImpl(
    private val recurringDao: RecurringTransactionDao
) : RecurringRepository {

    override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringDao.getAllRecurringTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringDao.getActiveRecurringTransactions().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long {
        return recurringDao.insertRecurringTransaction(recurring.toEntity())
    }

    override suspend fun updateRecurringTransaction(recurring: RecurringTransaction) {
        recurringDao.updateRecurringTransaction(recurring.toEntity())
    }

    override suspend fun deleteRecurringTransaction(id: Long) {
        recurringDao.deleteRecurringTransactionById(id)
    }

    override suspend fun toggleActive(id: Long, isActive: Boolean) {
        recurringDao.toggleActive(id, isActive)
    }

    // --- Mappers ---
    private fun RecurringTransactionEntity.toDomain() = RecurringTransaction(
        id = id,
        title = title,
        amount = amount,
        type = type,
        categoryId = categoryId,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        nextOccurrence = nextOccurrence,
        isActive = isActive,
        createdAt = createdAt
    )

    private fun RecurringTransaction.toEntity() = RecurringTransactionEntity(
        id = id,
        title = title,
        amount = amount,
        type = type,
        categoryId = categoryId,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        nextOccurrence = nextOccurrence,
        isActive = isActive,
        createdAt = createdAt
    )
}

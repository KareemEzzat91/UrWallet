package com.example.urwallet.features.more.data.repository

import androidx.room.withTransaction
import com.example.urwallet.core.database.UrWalletDatabase
import com.example.urwallet.features.more.data.dao.RecurringTransactionDao
import com.example.urwallet.features.more.data.entity.RecurringTransactionEntity
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.data.dao.TransactionDao
import com.example.urwallet.features.transactions.data.entity.TransactionEntity
import com.example.urwallet.features.transactions.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecurringRepositoryImpl @Inject constructor(
    private val database: UrWalletDatabase,
    private val recurringDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao
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

    override fun getRecurringTransactionById(id: Long): Flow<RecurringTransaction?> {
        return recurringDao.getRecurringTransactionById(id).map { it?.toDomain() }
    }

    override suspend fun getDueRecurringTransactionsSync(currentDate: Long): List<RecurringTransaction> {
        return recurringDao.getDueRecurringTransactionsSync(currentDate).map { it.toDomain() }
    }

    override suspend fun updateNextOccurrence(id: Long, nextOccurrence: Long) {
        recurringDao.updateNextOccurrence(id, nextOccurrence)
    }

    override suspend fun processOccurrence(
        recurringId: Long,
        occurrenceTag: String,
        transaction: Transaction,
        nextOccurrence: Long,
        hasEnded: Boolean
    ): Boolean {
        return database.withTransaction {
            val alreadyGenerated = transactionDao.countTransactionsByNoteTag("%$occurrenceTag%") > 0
            if (!alreadyGenerated) {
                transactionDao.insertTransaction(transaction.toEntity())
            }
            if (hasEnded) {
                recurringDao.toggleActive(recurringId, false)
            }
            recurringDao.updateNextOccurrence(recurringId, nextOccurrence)
            !alreadyGenerated
        }
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

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        amount = amount,
        type = type,
        categoryId = categoryId,
        title = title,
        note = note,
        date = date,
        receiptPath = receiptPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

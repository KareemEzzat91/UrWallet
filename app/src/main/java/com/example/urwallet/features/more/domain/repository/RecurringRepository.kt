package com.example.urwallet.features.more.domain.repository

import com.example.urwallet.features.more.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.emptyFlow

interface RecurringRepository {
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>>
    suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long
    suspend fun updateRecurringTransaction(recurring: RecurringTransaction)
    suspend fun deleteRecurringTransaction(id: Long)
    suspend fun toggleActive(id: Long, isActive: Boolean)
    fun getRecurringTransactionById(id: Long): Flow<RecurringTransaction?> = emptyFlow()
    suspend fun getDueRecurringTransactionsSync(currentDate: Long): List<RecurringTransaction> = emptyList()
    suspend fun updateNextOccurrence(id: Long, nextOccurrence: Long) {}
    suspend fun processOccurrence(
        recurringId: Long,
        occurrenceTag: String,
        transaction: com.example.urwallet.features.transactions.domain.model.Transaction,
        nextOccurrence: Long,
        hasEnded: Boolean
    ): Boolean = false
}

package com.example.urwallet.features.more.domain.repository

import com.example.urwallet.features.more.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>>
    suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long
    suspend fun updateRecurringTransaction(recurring: RecurringTransaction)
    suspend fun deleteRecurringTransaction(id: Long)
    suspend fun toggleActive(id: Long, isActive: Boolean)
}

package com.example.urwallet.features.more.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urwallet.features.more.data.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity): Long

    @Update
    suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity): Int

    @Delete
    suspend fun deleteRecurringTransaction(recurring: RecurringTransactionEntity): Int

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransactionById(id: Long): Int

    @Query("SELECT * FROM recurring_transactions ORDER BY nextOccurrence ASC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextOccurrence ASC")
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextOccurrence <= :currentDate")
    suspend fun getDueRecurringTransactionsSync(currentDate: Long): List<RecurringTransactionEntity>

    @Query("UPDATE recurring_transactions SET nextOccurrence = :nextOccurrence WHERE id = :id")
    suspend fun updateNextOccurrence(id: Long, nextOccurrence: Long): Int

    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE id = :id")
    suspend fun toggleActive(id: Long, isActive: Boolean): Int
}

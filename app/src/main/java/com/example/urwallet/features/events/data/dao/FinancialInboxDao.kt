package com.example.urwallet.features.events.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urwallet.features.events.data.entity.FinancialInboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialInboxDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: FinancialInboxEntity): Long

    @Update
    suspend fun updateEvent(event: FinancialInboxEntity): Int

    @Query("SELECT * FROM financial_inbox WHERE status = 'PENDING' ORDER BY date DESC")
    fun getPendingEvents(): Flow<List<FinancialInboxEntity>>

    @Query("SELECT * FROM financial_inbox ORDER BY date DESC")
    fun getAllEvents(): Flow<List<FinancialInboxEntity>>

    @Query("SELECT * FROM financial_inbox WHERE status = :status ORDER BY date DESC")
    fun getEventsByStatus(status: String): Flow<List<FinancialInboxEntity>>

    @Query("SELECT COUNT(*) FROM financial_inbox WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM financial_inbox WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): FinancialInboxEntity?

    @Query("SELECT * FROM financial_inbox WHERE sourceIdentifier = :sourceIdentifier LIMIT 1")
    suspend fun getEventBySourceIdentifier(sourceIdentifier: String): FinancialInboxEntity?

    @Query("UPDATE financial_inbox SET status = 'CONFIRMED', matchedTransactionId = :transactionId, rawMessage = NULL WHERE id = :id")
    suspend fun markConfirmedAndScrub(id: Long, transactionId: Long): Int

    @Query("UPDATE financial_inbox SET status = 'DISMISSED', rawMessage = NULL WHERE id = :id")
    suspend fun markDismissedAndScrub(id: Long): Int

    @Query("DELETE FROM financial_inbox WHERE id = :id")
    suspend fun deleteEventById(id: Long): Int
}

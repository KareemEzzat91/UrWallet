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

    @Query("""
        SELECT * FROM financial_inbox 
        WHERE (:status IS NULL OR status = :status)
          AND (
              :isDuplicateOnly = 0 OR 
              (status = 'PENDING' AND matchStatus IN ('POSSIBLE_MATCH', 'EXACT_MATCH'))
          )
          AND (:confidence IS NULL OR confidence = :confidence)
          AND (:sourceType IS NULL OR sourceType = :sourceType)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
          AND (
              :query IS NULL OR :query = '' OR 
              counterpartyName LIKE '%' || :query || '%' OR 
              phoneNumber LIKE '%' || :query || '%' OR 
              sender LIKE '%' || :query || '%' OR 
              accountOrCard LIKE '%' || :query || '%' OR
              CAST(amount AS TEXT) LIKE '%' || :query || '%'
          )
        ORDER BY 
          CASE WHEN :sortBy = 'DATE_DESC' THEN date END DESC,
          CASE WHEN :sortBy = 'DATE_ASC' THEN date END ASC,
          CASE WHEN :sortBy = 'AMOUNT_DESC' THEN amount END DESC,
          CASE WHEN :sortBy = 'AMOUNT_ASC' THEN amount END ASC,
          date DESC
    """)
    fun getFilteredEvents(
        status: String? = null,
        isDuplicateOnly: Int = 0,
        confidence: String? = null,
        sourceType: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        minAmount: Double? = null,
        maxAmount: Double? = null,
        query: String? = null,
        sortBy: String = "DATE_DESC"
    ): Flow<List<FinancialInboxEntity>>

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

    @Query("UPDATE financial_inbox SET matchStatus = 'NEW_EVENT', matchedTransactionId = NULL WHERE id = :id")
    suspend fun markAsDifferentTransaction(id: Long): Int

    @Query("UPDATE financial_inbox SET status = 'DISMISSED', rawMessage = NULL WHERE id IN (:ids)")
    suspend fun markDismissedBulk(ids: List<Long>): Int

    @Query("UPDATE financial_inbox SET status = 'PENDING' WHERE id IN (:ids)")
    suspend fun markPendingBulk(ids: List<Long>): Int

    @Query("DELETE FROM financial_inbox WHERE id = :id")
    suspend fun deleteEventById(id: Long): Int
}

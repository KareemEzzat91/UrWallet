package com.example.urwallet.features.transactions.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.data.entity.CategorySpendingEntity
import com.example.urwallet.features.transactions.data.entity.DailySpendingEntity
import com.example.urwallet.features.transactions.data.entity.MonthlyCashFlowEntity
import com.example.urwallet.features.transactions.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity): Int

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long): Int

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 4): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE personId IS NOT NULL ORDER BY date DESC")
    fun getTransactionsWithPeople(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT transactions.* FROM transactions
        LEFT JOIN categories ON transactions.categoryId = categories.id
        WHERE (:type IS NULL OR transactions.type = :type)
          AND (:startDate IS NULL OR transactions.date >= :startDate)
          AND (:endDate IS NULL OR transactions.date <= :endDate)
          AND (:minAmount IS NULL OR transactions.amount >= :minAmount)
          AND (:maxAmount IS NULL OR transactions.amount <= :maxAmount)
          AND (:hasCategories = 0 OR transactions.categoryId IN (:categoryIds))
          AND (
              :query IS NULL OR :query = '' OR
              transactions.title LIKE '%' || :query || '%' OR
              transactions.note LIKE '%' || :query || '%' OR
              categories.name LIKE '%' || :query || '%'
          )
        ORDER BY transactions.date DESC
    """)
    fun getFilteredTransactions(
        type: TransactionType? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        minAmount: Double? = null,
        maxAmount: Double? = null,
        hasCategories: Int = 0,
        categoryIds: List<Long> = emptyList(),
        query: String? = null
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type AND date >= :startDate AND date <= :endDate")
    fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE categoryId = :categoryId AND date >= :startDate AND date <= :endDate")
    fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type")
    fun getTotalSumByType(type: TransactionType): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions WHERE date >= :startOfDay AND date <= :endOfDay")
    fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE note LIKE :tagPattern")
    suspend fun countTransactionsByNoteTag(tagPattern: String): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions(): Int

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE personId = :personId ORDER BY date DESC")
    fun getTransactionsByPerson(personId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE personId = :personId ORDER BY date DESC")
    suspend fun getTransactionsByPersonSync(personId: Long): List<TransactionEntity>

    @Query("UPDATE transactions SET personId = NULL WHERE personId = :personId")
    suspend fun clearPersonFromTransactions(personId: Long): Int

    @Query("""
        SELECT categoryId, COALESCE(SUM(amount), 0.0) AS totalSpent 
        FROM transactions 
        WHERE type = 'EXPENSE' AND date >= :startDate AND date <= :endDate 
        GROUP BY categoryId
    """)
    fun getCategorySpendingBetween(startDate: Long, endDate: Long): Flow<List<CategorySpendingEntity>>

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: List<Long>): Int

    @Query("UPDATE transactions SET categoryId = :categoryId, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateCategoryByIds(ids: List<Long>, categoryId: Long, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("""
        SELECT 
            CAST(strftime('%Y', date / 1000, 'unixepoch', 'localtime') AS INTEGER) AS year,
            CAST(strftime('%m', date / 1000, 'unixepoch', 'localtime') AS INTEGER) AS month,
            type,
            COALESCE(SUM(amount), 0.0) AS totalAmount
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY year, month, type
        ORDER BY year ASC, month ASC
    """)
    fun getMonthlyCashFlowsBetween(startDate: Long, endDate: Long): Flow<List<MonthlyCashFlowEntity>>

    @Query("""
        SELECT 
            CAST(strftime('%Y', date / 1000, 'unixepoch', 'localtime') AS INTEGER) AS year,
            CAST(strftime('%m', date / 1000, 'unixepoch', 'localtime') AS INTEGER) AS month,
            CAST(strftime('%d', date / 1000, 'unixepoch', 'localtime') AS INTEGER) AS day,
            COALESCE(SUM(amount), 0.0) AS totalAmount
        FROM transactions
        WHERE type = 'EXPENSE' AND date >= :startDate AND date <= :endDate
        GROUP BY year, month, day
        ORDER BY year ASC, month ASC, day ASC
    """)
    fun getDailyExpensesBetween(startDate: Long, endDate: Long): Flow<List<DailySpendingEntity>>
}

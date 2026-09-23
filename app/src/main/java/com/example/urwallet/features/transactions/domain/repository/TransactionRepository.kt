package com.example.urwallet.features.transactions.domain.repository

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getFilteredTransactions(
        type: TransactionType? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        minAmount: Double? = null,
        maxAmount: Double? = null,
        categoryIds: List<Long> = emptyList(),
        query: String? = null
    ): Flow<List<Transaction>> = kotlinx.coroutines.flow.flowOf(emptyList())
    fun getTransactionsWithPeople(): Flow<List<Transaction>> = kotlinx.coroutines.flow.flowOf(emptyList())
    fun getTransactionById(id: Long): Flow<Transaction?>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    fun getTransactionsByPerson(personId: Long): Flow<List<Transaction>> = kotlinx.coroutines.flow.flowOf(emptyList())
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double>
    fun getTotalSumByType(type: TransactionType): Flow<Double>
    fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double>
    fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteTransactionById(id: Long)
    suspend fun countTransactionsByNoteTag(tagPattern: String): Int = 0

    // Category methods
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: Long)
}

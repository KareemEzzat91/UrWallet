package com.example.urwallet.features.transactions.domain.repository

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.data.entity.DailySpendingEntity
import com.example.urwallet.features.transactions.data.entity.MonthlyCashFlowEntity
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
    suspend fun deleteTransactionsByIds(ids: List<Long>): Int = 0
    suspend fun updateCategoryByIds(ids: List<Long>, categoryId: Long): Int = 0
    fun getCategorySpendingBetween(startDate: Long, endDate: Long): Flow<Map<Long, Double>> =
        kotlinx.coroutines.flow.flow {
            getTransactionsBetween(startDate, endDate).collect { txs ->
                emit(
                    txs.filter { it.type == TransactionType.EXPENSE && it.date in startDate..endDate }
                        .groupBy { it.categoryId }
                        .mapValues { (_, list) -> list.sumOf { it.amount } }
                )
            }
        }

    fun getMonthlyCashFlowsBetween(startDate: Long, endDate: Long): Flow<List<MonthlyCashFlowEntity>> =
        kotlinx.coroutines.flow.flow {
            getTransactionsBetween(startDate, endDate).collect { txs ->
                val cal = java.util.Calendar.getInstance()
                val filtered = txs.filter { it.date in startDate..endDate }
                val grouped = filtered.groupBy {
                    cal.timeInMillis = it.date
                    Triple(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, it.type)
                }.map { (key, list) ->
                    MonthlyCashFlowEntity(key.first, key.second, key.third, list.sumOf { it.amount })
                }
                emit(grouped)
            }
        }

    fun getDailyExpensesBetween(startDate: Long, endDate: Long): Flow<List<DailySpendingEntity>> =
        kotlinx.coroutines.flow.flow {
            getTransactionsBetween(startDate, endDate).collect { txs ->
                val cal = java.util.Calendar.getInstance()
                val filtered = txs.filter { it.type == TransactionType.EXPENSE && it.date in startDate..endDate }
                val grouped = filtered.groupBy {
                    cal.timeInMillis = it.date
                    Triple(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
                }.map { (key, list) ->
                    DailySpendingEntity(key.first, key.second, key.third, list.sumOf { it.amount })
                }
                emit(grouped)
            }
        }
    suspend fun countTransactionsByNoteTag(tagPattern: String): Int = 0

    // Category methods
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: Long)
}

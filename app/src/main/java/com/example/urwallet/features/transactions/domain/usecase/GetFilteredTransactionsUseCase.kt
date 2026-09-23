package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.FilterPeriod
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.model.TransactionFilterCriteria
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetFilteredTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    // Secondary constructor for in-memory testing without repository mock
    constructor() : this(object : TransactionRepository {
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(null)
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getSumByTypeAndPeriod(type: com.example.urwallet.core.common.TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTotalSumByType(type: com.example.urwallet.core.common.TransactionType): Flow<Double> = flowOf(0.0)
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(0)
        override suspend fun insertTransaction(transaction: Transaction): Long = 0L
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByType(type: com.example.urwallet.core.common.CategoryType): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: Long): Category? = null
        override suspend fun insertCategory(category: Category): Long = 0L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    })

    operator fun invoke(criteria: TransactionFilterCriteria): Flow<List<Transaction>> {
        val (periodStart, periodEnd) = resolvePeriod(criteria.period, criteria.customStartDate, criteria.customEndDate)
        return transactionRepository.getFilteredTransactions(
            type = criteria.type,
            startDate = periodStart,
            endDate = periodEnd,
            minAmount = criteria.minAmount,
            maxAmount = criteria.maxAmount,
            categoryIds = criteria.categoryIds.toList(),
            query = criteria.query
        )
    }

    private fun resolvePeriod(
        period: FilterPeriod,
        customStartDate: Long?,
        customEndDate: Long?
    ): Pair<Long?, Long?> {
        return when (period) {
            FilterPeriod.ALL -> null to null
            FilterPeriod.TODAY -> DateUtils.getStartOfDay() to DateUtils.getEndOfDay()
            FilterPeriod.THIS_WEEK -> DateUtils.getStartOfWeek() to DateUtils.getEndOfWeek()
            FilterPeriod.THIS_MONTH -> {
                val m = DateUtils.getCurrentMonth()
                val y = DateUtils.getCurrentYear()
                DateUtils.getStartOfMonth(m, y) to DateUtils.getEndOfMonth(m, y)
            }
            FilterPeriod.CUSTOM -> {
                val start = customStartDate ?: 0L
                val end = customEndDate ?: Long.MAX_VALUE
                start to end
            }
        }
    }

    operator fun invoke(
        transactions: List<Transaction>,
        categoryMap: Map<Long, Category>,
        criteria: TransactionFilterCriteria
    ): List<Transaction> {
        val query = criteria.query.trim().lowercase()
        val (periodStart, periodEnd) = resolvePeriod(criteria.period, criteria.customStartDate, criteria.customEndDate)

        return transactions.filter { tx ->
            // 1. Type filter
            if (criteria.type != null && tx.type != criteria.type) {
                return@filter false
            }

            // 2. Category filter
            if (criteria.categoryIds.isNotEmpty() && !criteria.categoryIds.contains(tx.categoryId)) {
                return@filter false
            }

            // 3. Period filter
            if (periodStart != null && periodEnd != null) {
                if (tx.date < periodStart || tx.date > periodEnd) {
                    return@filter false
                }
            }

            // 4. Amount bounds
            if (criteria.minAmount != null && tx.amount < criteria.minAmount) {
                return@filter false
            }
            if (criteria.maxAmount != null && tx.amount > criteria.maxAmount) {
                return@filter false
            }

            // 5. Search query matching: title, note, or category name
            if (query.isNotEmpty()) {
                val category = categoryMap[tx.categoryId]
                val matchTitle = tx.title.lowercase().contains(query)
                val matchNote = tx.note?.lowercase()?.contains(query) == true
                val matchCategory = category?.name?.lowercase()?.contains(query) == true

                if (!matchTitle && !matchNote && !matchCategory) {
                    return@filter false
                }
            }

            true
        }
    }
}

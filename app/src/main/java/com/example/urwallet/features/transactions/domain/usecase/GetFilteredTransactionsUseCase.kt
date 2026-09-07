package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.FilterPeriod
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.model.TransactionFilterCriteria
import javax.inject.Inject

// TODO(performance): For large datasets (>10,000 items), migrate filtering to Room queries
//   with Paging 3 (PagingSource). In-memory filtering is optimal and instantaneous for MVP scale.
class GetFilteredTransactionsUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<Transaction>,
        categoryMap: Map<Long, Category>,
        criteria: TransactionFilterCriteria
    ): List<Transaction> {
        val query = criteria.query.trim().lowercase()

        val (periodStart, periodEnd) = when (criteria.period) {
            FilterPeriod.ALL -> null to null
            FilterPeriod.TODAY -> DateUtils.getStartOfDay() to DateUtils.getEndOfDay()
            FilterPeriod.THIS_WEEK -> DateUtils.getStartOfWeek() to DateUtils.getEndOfWeek()
            FilterPeriod.THIS_MONTH -> {
                val m = DateUtils.getCurrentMonth()
                val y = DateUtils.getCurrentYear()
                DateUtils.getStartOfMonth(m, y) to DateUtils.getEndOfMonth(m, y)
            }
            FilterPeriod.CUSTOM -> {
                val start = criteria.customStartDate ?: 0L
                val end = criteria.customEndDate ?: Long.MAX_VALUE
                start to end
            }
        }

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

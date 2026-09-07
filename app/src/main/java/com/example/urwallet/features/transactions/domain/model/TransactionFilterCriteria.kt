package com.example.urwallet.features.transactions.domain.model

import com.example.urwallet.core.common.TransactionType

data class TransactionFilterCriteria(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryIds: Set<Long> = emptySet(),
    val period: FilterPeriod = FilterPeriod.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
) {
    fun hasActiveFilters(): Boolean {
        return query.isNotBlank() ||
                type != null ||
                categoryIds.isNotEmpty() ||
                period != FilterPeriod.ALL ||
                minAmount != null ||
                maxAmount != null
    }

    fun activeFilterCount(): Int {
        var count = 0
        if (type != null) count++
        if (categoryIds.isNotEmpty()) count++
        if (period != FilterPeriod.ALL) count++
        if (minAmount != null || maxAmount != null) count++
        return count
    }
}

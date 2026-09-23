package com.example.urwallet.features.events.domain.model

enum class InboxTab {
    ALL,
    PENDING,
    POSSIBLE_DUPLICATE,
    CONFIRMED,
    DISMISSED
}

enum class InboxSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    HIGHEST_AMOUNT,
    LOWEST_AMOUNT
}

data class InboxFilter(
    val tab: InboxTab = InboxTab.PENDING,
    val query: String = "",
    val confidence: EventConfidence? = null,
    val sourceType: FinancialEventSource? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val sortOrder: InboxSortOrder = InboxSortOrder.NEWEST_FIRST
)

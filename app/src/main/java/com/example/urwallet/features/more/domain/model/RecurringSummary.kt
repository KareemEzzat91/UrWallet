package com.example.urwallet.features.more.domain.model

data class RecurringSummary(
    val monthlyObligations: Double = 0.0,
    val monthlyRecurringIncome: Double = 0.0,
    val activeSubscriptionsCount: Int = 0,
    val items: List<RecurringTransactionWithCategory> = emptyList()
)

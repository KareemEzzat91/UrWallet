package com.example.urwallet.features.analytics.domain.model

data class DailyExpensePoint(
    val date: Long,
    val dayNumber: Int,
    val label: String,
    val amount: Double
)

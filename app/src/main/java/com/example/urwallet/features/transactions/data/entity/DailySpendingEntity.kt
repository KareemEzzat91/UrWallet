package com.example.urwallet.features.transactions.data.entity

data class DailySpendingEntity(
    val year: Int,
    val month: Int,
    val day: Int,
    val totalAmount: Double
)

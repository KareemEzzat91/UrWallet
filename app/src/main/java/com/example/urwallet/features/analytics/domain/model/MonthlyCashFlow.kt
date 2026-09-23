package com.example.urwallet.features.analytics.domain.model

data class MonthlyCashFlow(
    val monthName: String,
    val month: Int,
    val year: Int,
    val income: Double,
    val expense: Double,
    val net: Double
)

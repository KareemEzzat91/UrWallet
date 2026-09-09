package com.example.urwallet.features.analytics.domain.model

data class CategorySpendingSummary(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val amount: Double,
    val percentage: Double
)

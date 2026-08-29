package com.example.urwallet.features.analytics.domain.model

data class FinancialHealthScore(
    val overallScore: Int,      // 0 to 100
    val planningScore: Double,  // 0.0 to 100.0
    val savingScore: Double,    // 0.0 to 100.0
    val controlScore: Double    // 0.0 to 100.0
)

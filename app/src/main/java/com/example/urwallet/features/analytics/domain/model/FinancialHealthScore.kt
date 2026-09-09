package com.example.urwallet.features.analytics.domain.model

enum class HealthRating {
    EXCELLENT,          // 90..100: ممتاز
    VERY_GOOD,          // 75..89: جيد جداً
    AVERAGE,            // 60..74: متوسط
    NEEDS_IMPROVEMENT   // 0..59: يحتاج تحسين
}

data class FinancialHealthScore(
    val overallScore: Int,      // 0 to 100
    val planningScore: Int,     // 0 to 100
    val savingScore: Int,       // 0 to 100
    val controlScore: Int,      // 0 to 100
    val rating: HealthRating,
    val coachingMessage: String,
    val hasSufficientData: Boolean = true
)

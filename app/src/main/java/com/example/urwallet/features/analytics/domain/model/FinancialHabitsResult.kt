package com.example.urwallet.features.analytics.domain.model

enum class PersonalityType {
    SMART_PLANNER,      // المخطط الذكي 🧠
    CAUTIOUS_SAVER,     // المدخر الحذر 🛡️
    BALANCED,           // المتوازن ⚖️
    SPONTANEOUS_SPENDER // المستكشف ⚡
}

data class FinancialPersonality(
    val type: PersonalityType,
    val title: String,
    val emoji: String,
    val description: String,
    val coachRecommendation: String
)

data class Rule50_30_20Breakdown(
    val needsAmount: Double,
    val wantsAmount: Double,
    val savingsAmount: Double,
    val needsPercentage: Double,
    val wantsPercentage: Double,
    val savingsPercentage: Double,
    val targetNeedsPercentage: Double = 50.0,
    val targetWantsPercentage: Double = 30.0,
    val targetSavingsPercentage: Double = 20.0
)

data class FinancialHabitsResult(
    val personality: FinancialPersonality,
    val peakSpendingDay: String?,
    val peakSpendingAmount: Double,
    val dailyAverageSpend: Double,
    val rule50_30_20: Rule50_30_20Breakdown,
    val month: Int,
    val year: Int
)

package com.example.urwallet.features.events.domain.model

enum class CategorySuggestionSource {
    LEARNED_MAPPING,
    HISTORICAL_FREQUENCY,
    KEYWORD_RULES
}

data class CategorySuggestion(
    val categoryId: Long,
    val categoryName: String,
    val confidence: Float,
    val source: CategorySuggestionSource
)

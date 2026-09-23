package com.example.urwallet.features.events.domain.model

enum class DuplicateMatchStatus {
    NEW_EVENT,
    POSSIBLE_MATCH,
    EXACT_MATCH
}

data class DuplicateMatchResult(
    val status: DuplicateMatchStatus,
    val matchedTransactionId: Long? = null,
    val matchedTransactionTitle: String? = null,
    val matchedTransactionAmount: Double? = null,
    val matchedTransactionDate: Long? = null
)

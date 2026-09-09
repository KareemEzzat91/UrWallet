package com.example.urwallet.features.challenges.domain.model

enum class DayStatus {
    COMPLETED,
    FAILED,
    TODAY,
    PENDING
}

data class ChallengeDayStatus(
    val dayNumber: Int,
    val dateMillis: Long,
    val status: DayStatus,
    val spentAmount: Double = 0.0
)

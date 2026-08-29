package com.example.urwallet.features.challenges.domain.model

import com.example.urwallet.core.common.ChallengeType

data class Challenge(
    val id: Long = 0,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val targetAmount: Double? = null,
    val targetDays: Int? = null,
    val categoryId: Long? = null,
    val startDate: Long,
    val endDate: Long,
    val currentProgress: Double = 0.0,
    val streakDays: Int = 0,
    val isCompleted: Boolean = false,
    val isActive: Boolean = true
) {
    val progressPercentage: Double
        get() = when {
            targetAmount != null && targetAmount > 0 -> ((currentProgress / targetAmount) * 100.0).coerceIn(0.0, 100.0)
            targetDays != null && targetDays > 0 -> ((streakDays.toDouble() / targetDays) * 100.0).coerceIn(0.0, 100.0)
            else -> 0.0
        }
}

package com.example.urwallet.features.challenges.domain.model

data class ChallengeProgress(
    val challenge: Challenge,
    val currentProgress: Double,
    val targetAmount: Double?,
    val targetDays: Int?,
    val completedDays: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val remainingDays: Int,
    val progressPercentage: Double,
    val isCompleted: Boolean,
    val dailyStatuses: List<ChallengeDayStatus> = emptyList()
)

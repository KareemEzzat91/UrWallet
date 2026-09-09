package com.example.urwallet.features.challenges.domain.model

import com.example.urwallet.core.common.ChallengeType

data class ChallengePreset(
    val id: String,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val targetDays: Int? = null,
    val targetAmount: Double? = null,
    val categoryIcon: String? = null,
    val categoryName: String? = null,
    val difficulty: String,
    val durationText: String
)

package com.example.urwallet.features.challenges.domain.usecase

import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.features.challenges.domain.model.Challenge
import com.example.urwallet.features.challenges.domain.model.ChallengePreset
import com.example.urwallet.features.challenges.domain.repository.ChallengeRepository
import javax.inject.Inject

class JoinChallengeUseCase @Inject constructor(
    private val challengeRepository: ChallengeRepository
) {

    suspend operator fun invoke(
        preset: ChallengePreset,
        categoryId: Long? = null,
        customStartDate: Long? = null
    ): Long {
        val days = preset.targetDays ?: 7
        require(days > 0) { "Target days must be greater than zero" }

        if (preset.type == ChallengeType.SAVE_AMOUNT || preset.type == ChallengeType.REDUCE_CATEGORY) {
            val targetAmount = preset.targetAmount
            require(targetAmount != null && targetAmount > 0.0) {
                "Target amount must be specified and positive for this challenge type"
            }
        }

        val start = DateUtils.getStartOfDay(customStartDate ?: DateUtils.getCurrentEpochMs())
        val end = start + (days.toLong() * 24L * 60L * 60L * 1000L) - 1L

        val challenge = Challenge(
            id = 0L,
            title = preset.title,
            description = preset.description,
            type = preset.type,
            targetAmount = preset.targetAmount,
            targetDays = preset.targetDays,
            categoryId = categoryId,
            startDate = start,
            endDate = end,
            currentProgress = 0.0,
            streakDays = 0,
            isCompleted = false,
            isActive = true
        )

        return challengeRepository.insertChallenge(challenge)
    }
}

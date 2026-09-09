package com.example.urwallet.features.challenges.domain.usecase

import com.example.urwallet.features.challenges.domain.repository.ChallengeRepository
import javax.inject.Inject

class AbandonChallengeUseCase @Inject constructor(
    private val challengeRepository: ChallengeRepository
) {

    suspend operator fun invoke(challengeId: Long) {
        require(challengeId > 0L) { "Invalid challenge ID" }
        challengeRepository.deleteChallenge(challengeId)
    }
}

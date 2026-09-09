package com.example.urwallet.features.challenges.presentation

import com.example.urwallet.features.challenges.domain.model.ChallengePreset
import com.example.urwallet.features.challenges.domain.model.ChallengeProgress

sealed interface ChallengesUiState {
    data object Loading : ChallengesUiState

    data class Success(
        val activeChallenges: List<ChallengeProgress>,
        val completedChallenges: List<ChallengeProgress>,
        val presets: List<ChallengePreset>
    ) : ChallengesUiState

    data class Empty(
        val presets: List<ChallengePreset>
    ) : ChallengesUiState

    data class Error(
        val message: String
    ) : ChallengesUiState
}

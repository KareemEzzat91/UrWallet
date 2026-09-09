package com.example.urwallet.features.challenges.presentation

import com.example.urwallet.features.challenges.domain.model.ChallengeProgress

sealed interface ChallengeDetailUiState {
    data object Loading : ChallengeDetailUiState
    data class Success(val progress: ChallengeProgress) : ChallengeDetailUiState
    data class Error(val message: String) : ChallengeDetailUiState
    data object Deleted : ChallengeDetailUiState
}

package com.example.urwallet.features.challenges.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.challenges.domain.usecase.AbandonChallengeUseCase
import com.example.urwallet.features.challenges.domain.usecase.GetChallengeDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getChallengeDetailUseCase: GetChallengeDetailUseCase,
    private val abandonChallengeUseCase: AbandonChallengeUseCase
) : ViewModel() {

    private val challengeId: Long = savedStateHandle.get<Long>("challengeId") ?: -1L

    private val _isDeleted = MutableStateFlow(false)

    val uiState: StateFlow<ChallengeDetailUiState> = getChallengeDetailUseCase(challengeId)
        .map { progress ->
            if (_isDeleted.value) {
                ChallengeDetailUiState.Deleted
            } else if (progress != null) {
                ChallengeDetailUiState.Success(progress)
            } else {
                ChallengeDetailUiState.Error("لم يتم العثور على التحدي")
            }
        }
        .catch { error ->
            emit(ChallengeDetailUiState.Error(error.message ?: "حدث خطأ أثناء تحميل تفاصيل التحدي"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChallengeDetailUiState.Loading
        )

    fun abandonChallenge() {
        if (challengeId <= 0L) return
        viewModelScope.launch {
            try {
                abandonChallengeUseCase(challengeId)
                _isDeleted.value = true
            } catch (e: Exception) {
                // Handled in state
            }
        }
    }
}

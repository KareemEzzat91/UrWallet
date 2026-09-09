package com.example.urwallet.features.challenges.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.challenges.domain.model.ChallengePreset
import com.example.urwallet.features.challenges.domain.usecase.AbandonChallengeUseCase
import com.example.urwallet.features.challenges.domain.usecase.GetChallengePresetsUseCase
import com.example.urwallet.features.challenges.domain.usecase.GetChallengesUseCase
import com.example.urwallet.features.challenges.domain.usecase.JoinChallengeUseCase
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChallengesUiEvent {
    data class ChallengeJoined(val challengeId: Long) : ChallengesUiEvent
    data class ShowMessage(val message: String) : ChallengesUiEvent
}

@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val getChallengesUseCase: GetChallengesUseCase,
    private val getChallengePresetsUseCase: GetChallengePresetsUseCase,
    private val joinChallengeUseCase: JoinChallengeUseCase,
    private val abandonChallengeUseCase: AbandonChallengeUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiEvents = Channel<ChallengesUiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private val presets = getChallengePresetsUseCase()

    val challengesUiState: StateFlow<ChallengesUiState> = getChallengesUseCase()
        .map { result ->
            if (result.activeChallenges.isEmpty() && result.completedChallenges.isEmpty()) {
                ChallengesUiState.Empty(presets = presets)
            } else {
                ChallengesUiState.Success(
                    activeChallenges = result.activeChallenges,
                    completedChallenges = result.completedChallenges,
                    presets = presets
                )
            }
        }
        .catch { error ->
            emit(ChallengesUiState.Error(error.message ?: "حدث خطأ أثناء تحميل التحديات"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChallengesUiState.Loading
        )

    fun joinPreset(preset: ChallengePreset) {
        viewModelScope.launch {
            try {
                var categoryId: Long? = null
                if (preset.categoryName != null) {
                    val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                    val matched = categories.firstOrNull { it.name.contains(preset.categoryName, ignoreCase = true) }
                    categoryId = matched?.id
                }

                val id = joinChallengeUseCase(preset = preset, categoryId = categoryId)
                _uiEvents.send(ChallengesUiEvent.ChallengeJoined(id))
            } catch (e: Exception) {
                _uiEvents.send(ChallengesUiEvent.ShowMessage(e.message ?: "تعذر الانضمام للتحدي"))
            }
        }
    }

    fun abandonChallenge(challengeId: Long) {
        viewModelScope.launch {
            try {
                abandonChallengeUseCase(challengeId)
                _uiEvents.send(ChallengesUiEvent.ShowMessage("تم إنهاء التحدي"))
            } catch (e: Exception) {
                _uiEvents.send(ChallengesUiEvent.ShowMessage(e.message ?: "تعذر إنهاء التحدي"))
            }
        }
    }
}

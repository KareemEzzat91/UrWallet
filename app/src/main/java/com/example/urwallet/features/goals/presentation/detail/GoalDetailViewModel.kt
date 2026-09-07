package com.example.urwallet.features.goals.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.goals.domain.usecase.ContributeToGoalUseCase
import com.example.urwallet.features.goals.domain.usecase.DeleteGoalUseCase
import com.example.urwallet.features.goals.domain.usecase.GetGoalDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGoalDetailUseCase: GetGoalDetailUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val contributeToGoalUseCase: ContributeToGoalUseCase
) : ViewModel() {

    private val goalId = savedStateHandle.get<Long>("goalId") ?: -1L
    private val _goalIdFlow = MutableStateFlow(goalId)

    val detailUiState: StateFlow<GoalDetailUiState> = _goalIdFlow
        .flatMapLatest { id ->
            getGoalDetailUseCase(id).map { detail ->
                if (detail == null) {
                    GoalDetailUiState.Error("الهدف غير موجود أو تم حذفه")
                } else {
                    GoalDetailUiState.Success(detail)
                }
            }
        }
        .catch { e -> emit(GoalDetailUiState.Error(e.localizedMessage ?: "حدث خطأ أثناء تحميل بيانات الهدف")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = GoalDetailUiState.Loading
        )

    fun setGoalId(id: Long) {
        if (_goalIdFlow.value != id) {
            _goalIdFlow.value = id
        }
    }

    suspend fun contribute(amount: Double, note: String? = null): Result<Long> {
        val currentId = _goalIdFlow.value
        return contributeToGoalUseCase(
            goalId = currentId,
            amount = amount,
            note = note
        )
    }

    fun deleteGoal(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = deleteGoalUseCase(_goalIdFlow.value)
            onComplete(result.isSuccess)
        }
    }
}

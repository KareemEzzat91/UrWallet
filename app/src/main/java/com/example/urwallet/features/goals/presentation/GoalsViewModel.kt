package com.example.urwallet.features.goals.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.features.goals.domain.usecase.AddGoalUseCase
import com.example.urwallet.features.goals.domain.usecase.ContributeToGoalUseCase
import com.example.urwallet.features.goals.domain.usecase.DeleteGoalUseCase
import com.example.urwallet.features.goals.domain.usecase.GetGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    getGoalsUseCase: GetGoalsUseCase,
    private val addGoalUseCase: AddGoalUseCase,
    private val contributeToGoalUseCase: ContributeToGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase
) : ViewModel() {

    val goalsUiState: StateFlow<GoalsUiState> = getGoalsUseCase()
        .map { result ->
            if (result.activeGoals.isEmpty() && result.completedGoals.isEmpty()) {
                GoalsUiState.Empty
            } else {
                GoalsUiState.Success(
                    activeGoals = result.activeGoals,
                    completedGoals = result.completedGoals
                )
            }
        }
        .catch { e -> emit(GoalsUiState.Error(e.localizedMessage ?: "حدث خطأ أثناء تحميل الأهداف")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = GoalsUiState.Loading
        )

    suspend fun addGoal(
        name: String,
        targetAmount: Double,
        deadline: Long,
        paceMode: GoalPaceMode,
        icon: String
    ): Result<Long> {
        return addGoalUseCase(
            name = name,
            targetAmount = targetAmount,
            deadline = deadline,
            paceMode = paceMode,
            icon = icon
        )
    }

    suspend fun contributeToGoal(
        goalId: Long,
        amount: Double,
        note: String? = null
    ): Result<Long> {
        return contributeToGoalUseCase(
            goalId = goalId,
            amount = amount,
            note = note
        )
    }

    fun deleteGoal(goalId: Long, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = deleteGoalUseCase(goalId)
            onResult(result)
        }
    }
}

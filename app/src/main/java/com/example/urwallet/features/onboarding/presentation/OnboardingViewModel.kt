package com.example.urwallet.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.features.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.example.urwallet.features.onboarding.domain.usecase.SaveWizardGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class GoalType(val displayNameResId: Int, val defaultName: String, val iconName: String) {
    EMERGENCY(com.example.urwallet.R.string.goal_type_emergency, "صندوق الطوارئ", "🛡️"),
    TRAVEL(com.example.urwallet.R.string.goal_type_travel, "سفر وسياحة", "✈️"),
    HOME(com.example.urwallet.R.string.goal_type_home, "منزل جديد", "🏠"),
    RETIREMENT(com.example.urwallet.R.string.goal_type_retirement, "التقاعد", "⏰"),
    CUSTOM(com.example.urwallet.R.string.goal_type_custom, "هدف مخصص", "⭐")
}

data class WizardState(
    val selectedGoalType: GoalType = GoalType.EMERGENCY,
    val targetAmount: Double = 5000.0,
    val paceMode: GoalPaceMode = GoalPaceMode.BALANCED,
    val reminderWeekly: Boolean = true,
    val reminderMonthly: Boolean = false,
    val reminderDeadline: Boolean = true,
    val reminderMotivation: Boolean = false,
    val reminderHour: Int = 9,
    val monthsToGoal: Int = 0,
    val monthlyTarget: Double = 0.0
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val saveWizardGoalUseCase: SaveWizardGoalUseCase
) : ViewModel() {

    private val _wizardState = MutableStateFlow(WizardState())
    val wizardState: StateFlow<WizardState> = _wizardState.asStateFlow()

    private val _navigateToMain = MutableStateFlow(false)
    val navigateToMain: StateFlow<Boolean> = _navigateToMain.asStateFlow()

    init {
        calculatePlan()
    }

    fun setGoalType(type: GoalType) {
        _wizardState.update { it.copy(selectedGoalType = type) }
    }

    fun setTargetAmount(amount: Double) {
        _wizardState.update { it.copy(targetAmount = amount.coerceAtLeast(100.0)) }
        calculatePlan()
    }

    fun setPaceMode(mode: GoalPaceMode) {
        _wizardState.update { it.copy(paceMode = mode) }
        calculatePlan()
    }

    fun setReminderWeekly(enabled: Boolean) {
        _wizardState.update { it.copy(reminderWeekly = enabled) }
    }

    fun setReminderMonthly(enabled: Boolean) {
        _wizardState.update { it.copy(reminderMonthly = enabled) }
    }

    fun setReminderDeadline(enabled: Boolean) {
        _wizardState.update { it.copy(reminderDeadline = enabled) }
    }

    fun setReminderMotivation(enabled: Boolean) {
        _wizardState.update { it.copy(reminderMotivation = enabled) }
    }

    fun setReminderHour(hour: Int) {
        _wizardState.update { it.copy(reminderHour = hour) }
    }

    /** Derives monthsToGoal and monthlyTarget from current state */
    private fun calculatePlan() {
        val state = _wizardState.value
        val months = when (state.paceMode) {
            GoalPaceMode.RELAXED     -> 24
            GoalPaceMode.BALANCED    -> 12
            GoalPaceMode.AGGRESSIVE  -> 6
        }
        val monthly = if (months > 0) state.targetAmount / months else state.targetAmount
        _wizardState.update { it.copy(monthsToGoal = months, monthlyTarget = monthly) }
    }

    /** Skips the wizard — only marks onboarding completed */
    fun skipWizard() {
        viewModelScope.launch {
            completeOnboardingUseCase()
            _navigateToMain.value = true
        }
    }

    /** Saves the goal and completes onboarding */
    fun saveGoalAndComplete() {
        viewModelScope.launch {
            val state = _wizardState.value
            val deadline = Calendar.getInstance().apply {
                add(Calendar.MONTH, state.monthsToGoal)
            }.timeInMillis

            val goalName = state.selectedGoalType.defaultName

            saveWizardGoalUseCase(
                name = goalName,
                icon = state.selectedGoalType.iconName,
                targetAmount = state.targetAmount,
                paceMode = state.paceMode,
                monthlyTarget = state.monthlyTarget,
                deadlineEpochMs = deadline
            )
            completeOnboardingUseCase()
            _navigateToMain.value = true
        }
    }

    fun onNavigatedToMain() {
        _navigateToMain.value = false
    }
}

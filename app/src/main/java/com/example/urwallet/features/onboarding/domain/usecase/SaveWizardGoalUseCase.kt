package com.example.urwallet.features.onboarding.domain.usecase

import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import javax.inject.Inject

class SaveWizardGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(
        name: String,
        icon: String,
        targetAmount: Double,
        paceMode: GoalPaceMode,
        monthlyTarget: Double,
        deadlineEpochMs: Long
    ): Long {
        return goalRepository.insertGoal(
            Goal(
                name = name,
                icon = icon,
                targetAmount = targetAmount,
                paceMode = paceMode,
                monthlyTarget = monthlyTarget,
                deadline = deadlineEpochMs,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}

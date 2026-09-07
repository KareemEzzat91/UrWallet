package com.example.urwallet.features.goals.domain.usecase

import com.example.urwallet.features.goals.domain.calculator.GoalCalculator
import com.example.urwallet.features.goals.domain.model.GoalDetail
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetGoalDetailUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    operator fun invoke(goalId: Long): Flow<GoalDetail?> {
        return combine(
            goalRepository.getGoalById(goalId),
            goalRepository.getContributionsForGoal(goalId)
        ) { goal, contributions ->
            if (goal == null) return@combine null

            val daysRemaining = GoalCalculator.calculateDaysRemaining(goal.deadline)
            val requiredMonthlySavings = GoalCalculator.calculateRequiredMonthlySavings(
                targetAmount = goal.targetAmount,
                savedAmount = goal.savedAmount,
                deadline = goal.deadline
            )

            GoalDetail(
                goal = goal,
                savedAmount = goal.savedAmount,
                targetAmount = goal.targetAmount,
                remainingAmount = goal.remainingAmount,
                progressPercentage = goal.progressPercentage,
                isCompleted = goal.isCompleted,
                deadline = goal.deadline,
                daysRemaining = daysRemaining,
                requiredMonthlySavings = requiredMonthlySavings,
                contributions = contributions.sortedByDescending { it.date }
            )
        }
    }
}

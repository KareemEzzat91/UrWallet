package com.example.urwallet.features.goals.domain.usecase

import com.example.urwallet.features.goals.domain.repository.GoalRepository
import javax.inject.Inject

class DeleteGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(goalId: Long): Result<Unit> {
        return try {
            goalRepository.deleteGoal(goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

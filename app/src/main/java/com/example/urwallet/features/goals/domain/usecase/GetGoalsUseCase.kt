package com.example.urwallet.features.goals.domain.usecase

import com.example.urwallet.features.goals.domain.model.GoalsListResult
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    operator fun invoke(): Flow<GoalsListResult> {
        return goalRepository.getAllGoals().map { goals ->
            val active = goals.filter { !it.isCompleted }.sortedBy { it.deadline }
            val completed = goals.filter { it.isCompleted }.sortedByDescending { it.createdAt }
            GoalsListResult(activeGoals = active, completedGoals = completed)
        }
    }
}

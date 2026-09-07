package com.example.urwallet.features.goals.domain.usecase

import com.example.urwallet.features.goals.domain.repository.GoalRepository
import javax.inject.Inject

class ContributeToGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(
        goalId: Long,
        amount: Double,
        note: String? = null
    ): Result<Long> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("مبلغ الإيداع يجب أن يكون أكبر من صفر"))
        }

        return try {
            val contributionId = goalRepository.addContribution(
                goalId = goalId,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotBlank() }
            )
            Result.success(contributionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

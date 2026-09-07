package com.example.urwallet.features.goals.domain.usecase

import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.features.goals.domain.calculator.GoalCalculator
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import javax.inject.Inject

class AddGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(
        name: String,
        targetAmount: Double,
        deadline: Long,
        paceMode: GoalPaceMode = GoalPaceMode.BALANCED,
        icon: String = "ic_goal_custom"
    ): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الهدف"))
        }
        if (targetAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("المبلغ المستهدف يجب أن يكون أكبر من صفر"))
        }
        if (deadline <= System.currentTimeMillis()) {
            return Result.failure(IllegalArgumentException("تاريخ الوصول للهدف يجب أن يكون في المستقبل"))
        }

        val monthlyTarget = GoalCalculator.calculateRequiredMonthlySavings(
            targetAmount = targetAmount,
            savedAmount = 0.0,
            deadline = deadline
        )

        val goal = Goal(
            name = name.trim(),
            icon = icon,
            targetAmount = targetAmount,
            savedAmount = 0.0,
            paceMode = paceMode,
            monthlyTarget = monthlyTarget,
            deadline = deadline,
            createdAt = System.currentTimeMillis()
        )

        return try {
            val id = goalRepository.insertGoal(goal)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

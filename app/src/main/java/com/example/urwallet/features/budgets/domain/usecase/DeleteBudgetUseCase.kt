package com.example.urwallet.features.budgets.domain.usecase

import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import javax.inject.Inject

class DeleteBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {

    suspend operator fun invoke(budgetId: Long): Result<Unit> {
        return try {
            budgetRepository.deleteBudget(budgetId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

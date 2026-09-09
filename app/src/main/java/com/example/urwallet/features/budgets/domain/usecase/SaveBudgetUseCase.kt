package com.example.urwallet.features.budgets.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import javax.inject.Inject

class SaveBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend operator fun invoke(
        categoryId: Long?,
        amount: Double,
        month: Int,
        year: Int,
        alertThreshold: Double = 0.80
    ): Result<Long> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("يرجى إدخال مبلغ ميزانية صحيح أكبر من صفر"))
        }

        if (month !in 1..12 || year < 2000) {
            return Result.failure(IllegalArgumentException("تاريخ الميزانية غير صالح"))
        }

        if (categoryId != null) {
            val category = categoryRepository.getCategoryById(categoryId)
                ?: return Result.failure(IllegalArgumentException("الفئة المحددة غير موجودة"))

            if (category.type != CategoryType.EXPENSE) {
                return Result.failure(IllegalArgumentException("لا يمكن إنشاء ميزانية لفئة دخل"))
            }
        }

        return try {
            val budget = Budget(
                categoryId = categoryId,
                amount = amount,
                month = month,
                year = year,
                alertThreshold = alertThreshold
            )
            val id = budgetRepository.insertOrUpdateBudget(budget)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

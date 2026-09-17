package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(categoryId: Long): Result<Unit> {
        if (categoryId <= 0) {
            return Result.failure(IllegalArgumentException("معرف التصنيف غير صالح"))
        }

        val existing = categoryRepository.getCategoryById(categoryId)
            ?: return Result.failure(IllegalArgumentException("التصنيف غير موجود"))

        if (existing.isDefault) {
            return Result.failure(IllegalStateException("لا يمكن حذف التصنيفات الأساسية للنظام"))
        }

        return try {
            categoryRepository.deleteCategory(categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

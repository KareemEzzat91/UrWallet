package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        id: Long,
        name: String,
        type: CategoryType,
        icon: String,
        color: String
    ): Result<Unit> {
        if (id <= 0) {
            return Result.failure(IllegalArgumentException("معرف التصنيف غير صالح"))
        }
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("اسم التصنيف لا يمكن أن يكون فارغاً"))
        }

        val existing = categoryRepository.getCategoryById(id)
            ?: return Result.failure(IllegalArgumentException("التصنيف غير موجود"))

        val updated = existing.copy(
            name = trimmedName,
            type = type,
            icon = icon.ifBlank { existing.icon },
            color = color.ifBlank { existing.color }
        )

        return try {
            categoryRepository.updateCategory(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

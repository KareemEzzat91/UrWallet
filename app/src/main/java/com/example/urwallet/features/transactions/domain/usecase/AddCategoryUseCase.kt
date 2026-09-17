package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        name: String,
        type: CategoryType,
        icon: String = "ic_other",
        color: String = "#78909C"
    ): Result<Long> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("اسم التصنيف لا يمكن أن يكون فارغاً"))
        }

        val category = Category(
            name = trimmedName,
            type = type,
            icon = icon.ifBlank { "ic_other" },
            color = color.ifBlank { "#78909C" },
            isDefault = false,
            isDeleted = false
        )

        return try {
            val id = categoryRepository.insertCategory(category)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

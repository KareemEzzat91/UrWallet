package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import javax.inject.Inject

class BulkChangeCategoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(transactionIds: Set<Long>, newCategoryId: Long): Result<Int> {
        val validIds = transactionIds.filter { it > 0L }
        if (validIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("لم يتم تحديد أي معاملات"))
        }
        if (newCategoryId <= 0L) {
            return Result.failure(IllegalArgumentException("يرجى اختيار تصنيف صالح"))
        }
        val category = categoryRepository.getCategoryById(newCategoryId)
            ?: return Result.failure(IllegalArgumentException("التصنيف المحدد غير موجود"))

        return try {
            val updatedCount = transactionRepository.updateCategoryByIds(validIds, category.id)
            Result.success(updatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

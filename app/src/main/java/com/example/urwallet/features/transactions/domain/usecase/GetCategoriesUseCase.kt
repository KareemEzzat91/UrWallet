package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(type: CategoryType): Flow<List<Category>> {
        return transactionRepository.getCategoriesByType(type)
    }

    operator fun invoke(): Flow<List<Category>> {
        return transactionRepository.getAllCategories()
    }
}

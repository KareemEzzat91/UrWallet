package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import javax.inject.Inject

class BulkDeleteTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transactionIds: Set<Long>): Result<Int> {
        val validIds = transactionIds.filter { it > 0L }
        if (validIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("لم يتم تحديد أي معاملات للحذف"))
        }
        return try {
            val deletedCount = transactionRepository.deleteTransactionsByIds(validIds)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

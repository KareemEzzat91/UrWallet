package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        id: Long,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        title: String,
        note: String? = null,
        date: Long,
        receiptPath: String? = null
    ): Result<Unit> {
        if (id <= 0L) {
            return Result.failure(IllegalArgumentException("معرف المعاملة غير صالح"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر"))
        }
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("اسم المعاملة لا يمكن أن يكون فارغاً"))
        }
        if (categoryId <= 0L) {
            return Result.failure(IllegalArgumentException("يرجى اختيار تصنيف للمعاملة"))
        }

        return try {
            val transaction = Transaction(
                id = id,
                amount = amount,
                type = type,
                categoryId = categoryId,
                title = title.trim(),
                note = note?.trim()?.ifBlank { null },
                date = date,
                receiptPath = receiptPath,
                updatedAt = System.currentTimeMillis()
            )
            transactionRepository.updateTransaction(transaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

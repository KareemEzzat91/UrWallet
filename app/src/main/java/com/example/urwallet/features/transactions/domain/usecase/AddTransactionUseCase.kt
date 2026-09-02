package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        title: String,
        note: String? = null,
        date: Long = System.currentTimeMillis(),
        receiptPath: String? = null
    ): Result<Long> {
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
                amount = amount,
                type = type,
                categoryId = categoryId,
                title = title.trim(),
                note = note?.trim()?.ifBlank { null },
                date = date,
                receiptPath = receiptPath,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = transactionRepository.insertTransaction(transaction)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

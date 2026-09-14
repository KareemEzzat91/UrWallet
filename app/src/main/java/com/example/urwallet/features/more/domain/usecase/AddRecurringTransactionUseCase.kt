package com.example.urwallet.features.more.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import javax.inject.Inject

class AddRecurringTransactionUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository
) {

    suspend operator fun invoke(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        frequency: Frequency,
        startDate: Long = System.currentTimeMillis(),
        endDate: Long? = null
    ): Long {
        require(title.isNotBlank()) { "عنوان المعاملة لا يمكن أن يكون فارغاً" }
        require(amount > 0.0) { "المبلغ يجب أن يكون أكبر من صفر" }
        require(categoryId > 0) { "يجب اختيار فئة صحيحة" }
        if (endDate != null) {
            require(endDate >= startDate) { "تاريخ الانتهاء يجب أن يكون بعد تاريخ البدء" }
        }

        val normalizedStart = DateUtils.getStartOfDay(startDate)
        val normalizedEnd = endDate?.let { DateUtils.getEndOfDay(it) }

        val recurring = RecurringTransaction(
            title = title.trim(),
            amount = amount,
            type = type,
            categoryId = categoryId,
            frequency = frequency,
            startDate = normalizedStart,
            endDate = normalizedEnd,
            nextOccurrence = normalizedStart,
            isActive = true
        )

        return recurringRepository.insertRecurringTransaction(recurring)
    }
}

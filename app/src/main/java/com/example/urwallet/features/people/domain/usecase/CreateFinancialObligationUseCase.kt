package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationStatus
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import javax.inject.Inject

class CreateFinancialObligationUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository
) {
    suspend operator fun invoke(
        personId: Long,
        amount: Double,
        direction: ObligationDirection,
        reason: String? = null,
        dueDate: Long? = null,
        relatedTransactionId: Long? = null
    ): Result<Long> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("مبلغ الالتزام المالي يجب أن يكون أكبر من صفر"))
        }

        val person = peopleRepository.getPersonByIdSync(personId)
            ?: return Result.failure(IllegalArgumentException("الشخص المحدد غير موجود"))

        return runCatching {
            peopleRepository.insertObligation(
                FinancialObligation(
                    personId = person.id,
                    amount = amount,
                    direction = direction,
                    reason = reason?.trim()?.ifBlank { null },
                    dueDate = dueDate,
                    status = ObligationStatus.OPEN,
                    settledAmount = 0.0,
                    remainingAmount = amount,
                    relatedTransactionId = relatedTransactionId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

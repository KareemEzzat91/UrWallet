package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationSettlement
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import javax.inject.Inject

class SettleObligationUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository
) {
    suspend operator fun invoke(
        obligationId: Long,
        amount: Double,
        note: String? = null,
        date: Long = System.currentTimeMillis(),
        relatedTransactionId: Long? = null
    ): Result<FinancialObligation> {
        val obligation = peopleRepository.getObligationByIdSync(obligationId)
            ?: return Result.failure(IllegalArgumentException("الالتزام المالي غير موجود"))

        if (obligation.isSettled) {
            return Result.failure(IllegalStateException("هذا الالتزام المالي مسدد بالكامل بالفعل"))
        }

        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("مبلغ التسوية يجب أن يكون أكبر من صفر"))
        }

        if (amount > obligation.remainingAmount + 0.001) {
            return Result.failure(
                IllegalArgumentException("مبلغ التسوية ($amount) لا يمكن أن يتجاوز المبلغ المتبقي (${obligation.remainingAmount})")
            )
        }

        return runCatching {
            // Atomic execution inside Room @Transaction
            peopleRepository.recordSettlementAtomic(
                ObligationSettlement(
                    obligationId = obligationId,
                    amount = amount,
                    date = date,
                    note = note?.trim()?.ifBlank { null },
                    relatedTransactionId = relatedTransactionId
                )
            )
        }
    }
}

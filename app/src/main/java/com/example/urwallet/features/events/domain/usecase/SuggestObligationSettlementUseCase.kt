package com.example.urwallet.features.events.domain.usecase

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.ObligationSettlementSuggestion
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationStatus
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import javax.inject.Inject
import kotlin.math.abs

class SuggestObligationSettlementUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository
) {

    suspend operator fun invoke(
        personId: Long?,
        amount: Double,
        type: TransactionType
    ): List<ObligationSettlementSuggestion> {
        if (personId == null || personId <= 0L || amount <= 0.0) {
            return emptyList()
        }

        // 1. Determine matching obligation direction
        val targetDirection = if (type == TransactionType.INCOME) {
            ObligationDirection.OWED_TO_ME // Received money -> settles money they owed me
        } else {
            ObligationDirection.I_OWE // Sent money -> settles money I owed them
        }

        // 2. Fetch active obligations for this person
        val activeObligations = peopleRepository.getActiveObligationsByPersonSync(personId)
            .filter { it.direction == targetDirection && it.status != ObligationStatus.SETTLED && it.remainingAmount > 0.001 }

        if (activeObligations.isEmpty()) {
            return emptyList()
        }

        // 3. Map into conservative settlement suggestions
        return activeObligations.map { ob ->
            val isExact = abs(ob.remainingAmount - amount) < 0.01
            val settleAmount = amount.coerceAtMost(ob.remainingAmount)
            val reason = when {
                isExact -> "المبلغ يطابق تماماً المبلغ المتبقي (${ob.remainingAmount} ج.م)"
                amount < ob.remainingAmount -> "سداد جزئي (متبقي بعد السداد: ${ob.remainingAmount - amount} ج.م)"
                else -> "تسوية لكامل الالتزام المتبقي (${ob.remainingAmount} ج.م)"
            }

            ObligationSettlementSuggestion(
                obligation = ob,
                suggestedAmount = settleAmount,
                matchReason = reason,
                isExactAmountMatch = isExact
            )
        }.sortedWith(
            compareByDescending<ObligationSettlementSuggestion> { it.isExactAmountMatch }
                .thenBy { abs(it.obligation.remainingAmount - amount) }
        )
    }
}

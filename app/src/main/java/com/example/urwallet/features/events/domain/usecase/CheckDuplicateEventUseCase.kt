package com.example.urwallet.features.events.domain.usecase

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.DuplicateMatchResult
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

class CheckDuplicateEventUseCase @Inject constructor(
    private val financialEventRepository: FinancialEventRepository,
    private val transactionRepository: TransactionRepository
) {

    private val WINDOW_MILLIS = TimeUnit.HOURS.toMillis(36)
    private val EXACT_WINDOW_MILLIS = TimeUnit.HOURS.toMillis(2)

    suspend operator fun invoke(
        amount: Double,
        type: TransactionType,
        date: Long,
        sourceIdentifier: String,
        counterpartyName: String? = null
    ): DuplicateMatchResult {
        // 1. Check exact source idempotency in financial inbox
        val existingInbox = financialEventRepository.getEventBySourceIdentifier(sourceIdentifier)
        if (existingInbox != null) {
            return DuplicateMatchResult(
                status = DuplicateMatchStatus.EXACT_MATCH,
                matchedTransactionId = existingInbox.matchedTransactionId
            )
        }

        // 2. Query existing transactions in date window [date - 36h, date + 36h]
        val startWindow = date - WINDOW_MILLIS
        val endWindow = date + WINDOW_MILLIS

        val existingTransactions = transactionRepository.getTransactionsBetween(startWindow, endWindow).first()

        for (tx in existingTransactions) {
            val amountMatches = abs(tx.amount - amount) < 0.01
            val typeMatches = tx.type == type

            if (amountMatches && typeMatches) {
                val timeDiff = abs(tx.date - date)
                val isNearbyInTime = timeDiff <= EXACT_WINDOW_MILLIS

                val titleMatches = !counterpartyName.isNullOrBlank() && (
                    tx.title.contains(counterpartyName, ignoreCase = true) ||
                    (tx.note?.contains(counterpartyName, ignoreCase = true) == true)
                )

                if (isNearbyInTime && titleMatches) {
                    return DuplicateMatchResult(
                        status = DuplicateMatchStatus.EXACT_MATCH,
                        matchedTransactionId = tx.id,
                        matchedTransactionTitle = tx.title,
                        matchedTransactionAmount = tx.amount,
                        matchedTransactionDate = tx.date
                    )
                }

                // If amount and type match within 36 hours, mark as POSSIBLE_MATCH for user review
                return DuplicateMatchResult(
                    status = DuplicateMatchStatus.POSSIBLE_MATCH,
                    matchedTransactionId = tx.id,
                    matchedTransactionTitle = tx.title,
                    matchedTransactionAmount = tx.amount,
                    matchedTransactionDate = tx.date
                )
            }
        }

        return DuplicateMatchResult(status = DuplicateMatchStatus.NEW_EVENT)
    }
}

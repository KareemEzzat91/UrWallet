package com.example.urwallet.features.more.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.features.more.domain.calculator.NextOccurrenceCalculator
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Idempotent processor for due recurring transactions.
 * Follows Strategy B (MVP): Processes the next due occurrence per run, advances nextOccurrence,
 * and guarantees exact-once execution via deterministic occurrence tagging.
 */
class ProcessDueRecurringTransactionsUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(currentTime: Long = System.currentTimeMillis()): Int {
        val endOfCurrentDay = DateUtils.getEndOfDay(currentTime)
        val dueList = recurringRepository.getDueRecurringTransactionsSync(endOfCurrentDay)
        var generatedCount = 0

        for (recurring in dueList) {
            if (!recurring.isActive) continue

            // 1. Normalize occurrence date to the beginning of the scheduled day
            val occurrenceDate = DateUtils.getStartOfDay(recurring.nextOccurrence)

            // 2. Deterministic occurrence tag to guarantee strict idempotency
            val occurrenceTag = "[REC:#${recurring.id}@$occurrenceDate]"

            // 3. Check if this occurrence was already generated
            val alreadyGenerated = transactionRepository.countTransactionsByNoteTag("%$occurrenceTag%") > 0

            if (!alreadyGenerated) {
                // Insert actual financial transaction
                val transaction = Transaction(
                    amount = recurring.amount,
                    type = recurring.type,
                    categoryId = recurring.categoryId,
                    title = recurring.title,
                    note = "$occurrenceTag معاملة مجدولة آلياً",
                    date = occurrenceDate
                )
                transactionRepository.insertTransaction(transaction)
                generatedCount++
            }

            // 4. Calculate next occurrence
            val nextDate = NextOccurrenceCalculator.calculateNextOccurrence(
                currentOccurrence = recurring.nextOccurrence,
                frequency = recurring.frequency,
                originalStartDate = recurring.startDate
            )

            // 5. Check if recurring series has reached its end date
            val hasEnded = recurring.endDate != null && nextDate > recurring.endDate

            if (hasEnded) {
                recurringRepository.toggleActive(recurring.id, false)
            }

            // 6. Advance nextOccurrence
            recurringRepository.updateNextOccurrence(recurring.id, nextDate)
        }

        return generatedCount
    }
}

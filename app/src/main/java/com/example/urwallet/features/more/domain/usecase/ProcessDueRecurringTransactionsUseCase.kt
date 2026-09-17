package com.example.urwallet.features.more.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.features.more.domain.calculator.NextOccurrenceCalculator
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.domain.model.Transaction
import javax.inject.Inject

/**
 * Idempotent processor for due recurring transactions.
 * Follows Strategy B (MVP): Processes the next due occurrence per run, advances nextOccurrence,
 * and guarantees exact-once execution via deterministic occurrence tagging.
 * All occurrence checks, transaction inserts, and nextOccurrence updates are executed atomically.
 */
class ProcessDueRecurringTransactionsUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository
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

            // 3. Prepare actual financial transaction
            val transaction = Transaction(
                amount = recurring.amount,
                type = recurring.type,
                categoryId = recurring.categoryId,
                title = recurring.title,
                note = "$occurrenceTag معاملة مجدولة آلياً",
                date = occurrenceDate
            )

            // 4. Calculate next occurrence
            val nextDate = NextOccurrenceCalculator.calculateNextOccurrence(
                currentOccurrence = recurring.nextOccurrence,
                frequency = recurring.frequency,
                originalStartDate = recurring.startDate
            )

            // 5. Check if recurring series has reached its end date
            val hasEnded = recurring.endDate != null && nextDate > recurring.endDate

            // 6. Atomically check existence, insert transaction, update series status and advance nextOccurrence
            val wasGenerated = recurringRepository.processOccurrence(
                recurringId = recurring.id,
                occurrenceTag = occurrenceTag,
                transaction = transaction,
                nextOccurrence = nextDate,
                hasEnded = hasEnded
            )

            if (wasGenerated) {
                generatedCount++
            }
        }

        return generatedCount
    }
}

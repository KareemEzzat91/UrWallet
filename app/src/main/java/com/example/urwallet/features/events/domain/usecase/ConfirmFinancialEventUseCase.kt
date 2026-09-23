package com.example.urwallet.features.events.domain.usecase

import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyMapping
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import com.example.urwallet.features.transactions.domain.usecase.AddTransactionUseCase
import javax.inject.Inject

class ConfirmFinancialEventUseCase @Inject constructor(
    private val financialEventRepository: FinancialEventRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) {

    suspend operator fun invoke(
        eventId: Long,
        categoryId: Long,
        title: String,
        note: String? = null,
        date: Long? = null,
        counterparty: Counterparty? = null,
        personId: Long? = null,
        saveCounterpartyMapping: Boolean = true
    ): Result<Long> {
        val event = financialEventRepository.getEventById(eventId)
            ?: return Result.failure(IllegalArgumentException("لم يتم العثور على المعاملة في الوارد المالي"))

        val eventDate = date ?: event.date
        val finalTitle = title.trim().ifBlank {
            counterparty?.name ?: event.sender
        }

        // 1. Create real transaction via the existing AddTransactionUseCase
        val addResult = addTransactionUseCase(
            amount = event.amount,
            type = event.type,
            categoryId = categoryId,
            title = finalTitle,
            note = note?.trim()?.ifBlank { null },
            date = eventDate,
            personId = personId
        )

        return addResult.mapCatching { transactionId ->
            // 2. Optionally remember the phone-to-counterparty mapping for future detections
            val phone = counterparty?.phoneNumber
            if (saveCounterpartyMapping && !phone.isNullOrBlank() && !counterparty.name.isBlank()) {
                financialEventRepository.saveMapping(
                    CounterpartyMapping(
                        phoneNumber = phone,
                        name = counterparty.name.trim(),
                        type = counterparty.type,
                        personId = personId
                    )
                )
            }

            // 3. Mark inbox event as CONFIRMED and scrub rawMessage for privacy
            financialEventRepository.markConfirmed(eventId, transactionId)

            transactionId
        }
    }
}

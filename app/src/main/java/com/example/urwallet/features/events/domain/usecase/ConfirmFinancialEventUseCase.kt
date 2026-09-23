package com.example.urwallet.features.events.domain.usecase

import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import javax.inject.Inject

class ConfirmFinancialEventUseCase @Inject constructor(
    private val financialEventRepository: FinancialEventRepository
) {

    suspend operator fun invoke(
        eventId: Long,
        categoryId: Long,
        title: String,
        note: String? = null,
        date: Long? = null,
        counterparty: Counterparty? = null,
        personId: Long? = null,
        saveCounterpartyMapping: Boolean = true,
        saveCategoryMapping: Boolean = true,
        settleObligationId: Long? = null
    ): Result<Long> {
        val event = financialEventRepository.getEventById(eventId)
            ?: return Result.failure(IllegalArgumentException("لم يتم العثور على المعاملة في الوارد المالي"))

        if (categoryId <= 0L) {
            return Result.failure(IllegalArgumentException("يرجى اختيار تصنيف للمعاملة"))
        }

        val eventDate = date ?: event.date
        val finalTitle = title.trim().ifBlank {
            counterparty?.name ?: event.sender
        }

        val learnedCategoryPattern = if (saveCategoryMapping) {
            counterparty?.name?.trim()?.ifBlank { null } ?: event.sender.trim()
        } else null

        // Executes transaction creation, optional obligation settlement, mapping updates,
        // and inbox confirmation atomically inside ONE Room transaction
        return financialEventRepository.confirmEventAtomic(
            eventId = eventId,
            categoryId = categoryId,
            title = finalTitle,
            amount = event.amount,
            type = event.type,
            date = eventDate,
            note = note?.trim()?.ifBlank { null },
            personId = personId,
            counterparty = counterparty,
            saveCounterpartyMapping = saveCounterpartyMapping,
            learnedCategoryPattern = learnedCategoryPattern,
            settleObligationId = settleObligationId
        )
    }
}

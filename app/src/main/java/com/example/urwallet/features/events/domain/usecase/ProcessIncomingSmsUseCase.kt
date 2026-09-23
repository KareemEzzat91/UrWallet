package com.example.urwallet.features.events.domain.usecase

import com.example.urwallet.features.events.data.parser.ExtractorRegistry
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyType
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.repository.ContactResolutionRepository
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import javax.inject.Inject

class ProcessIncomingSmsUseCase @Inject constructor(
    private val extractorRegistry: ExtractorRegistry,
    private val financialEventRepository: FinancialEventRepository,
    private val contactResolutionRepository: ContactResolutionRepository,
    private val checkDuplicateEventUseCase: CheckDuplicateEventUseCase
) {

    suspend operator fun invoke(
        sender: String,
        message: String,
        timestamp: Long,
        originalSmsId: String? = null
    ): FinancialEvent? {
        // 1. Extract partial event using bank strategies or generic fallback
        val partial = extractorRegistry.extract(sender, message, timestamp, originalSmsId) ?: return null

        // 2. Check source idempotency
        val existing = financialEventRepository.getEventBySourceIdentifier(partial.sourceIdentifier)
        if (existing != null) {
            // Already processed this exact SMS before
            return null
        }

        // 3. Resolve Counterparty
        var resolvedCounterparty: Counterparty? = null
        val phone = partial.rawPhoneNumber

        if (!phone.isNullOrBlank()) {
            // 3a. Check local learned phone mapping first
            val learned = financialEventRepository.getMappingForPhone(phone)
            if (learned != null) {
                resolvedCounterparty = Counterparty(
                    name = learned.name,
                    type = learned.type,
                    phoneNumber = phone
                )
            } else if (contactResolutionRepository.hasContactsPermission()) {
                // 3b. Optional fallback to Android Contacts for this specific number
                val contactName = contactResolutionRepository.resolveContactName(phone)
                if (!contactName.isNullOrBlank()) {
                    resolvedCounterparty = Counterparty(
                        name = contactName,
                        type = CounterpartyType.PERSON,
                        phoneNumber = phone
                    )
                } else {
                    resolvedCounterparty = Counterparty(
                        name = phone,
                        type = CounterpartyType.PERSON,
                        phoneNumber = phone
                    )
                }
            } else {
                resolvedCounterparty = Counterparty(
                    name = phone,
                    type = CounterpartyType.PERSON,
                    phoneNumber = phone
                )
            }
        } else if (!partial.rawCounterparty.isNullOrBlank()) {
            resolvedCounterparty = Counterparty(
                name = partial.rawCounterparty,
                type = CounterpartyType.MERCHANT,
                phoneNumber = null
            )
        }

        // 4. Check for duplicate or same-event against existing Room transactions
        val matchResult = checkDuplicateEventUseCase(
            amount = partial.amount,
            type = partial.type,
            date = partial.date,
            sourceIdentifier = partial.sourceIdentifier,
            counterpartyName = resolvedCounterparty?.name
        )

        // If exact duplicate of already processed source, drop it
        if (matchResult.status == DuplicateMatchStatus.EXACT_MATCH && matchResult.matchedTransactionId == null) {
            return null
        }

        // 5. Construct domain FinancialEvent
        val event = FinancialEvent(
            sourceType = partial.source,
            sourceIdentifier = partial.sourceIdentifier,
            rawMessage = partial.rawMessage,
            sender = partial.sender,
            amount = partial.amount,
            currency = "EGP",
            type = partial.type,
            date = partial.date,
            accountOrCard = partial.accountOrCard,
            counterparty = resolvedCounterparty,
            suggestedCategoryId = null,
            confidence = partial.confidence,
            status = InboxStatus.PENDING,
            matchStatus = matchResult.status,
            matchedTransactionId = matchResult.matchedTransactionId,
            createdAt = System.currentTimeMillis()
        )

        val id = financialEventRepository.insertEvent(event)
        return event.copy(id = id)
    }
}

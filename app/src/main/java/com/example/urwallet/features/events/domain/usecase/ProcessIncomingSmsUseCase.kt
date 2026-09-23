package com.example.urwallet.features.events.domain.usecase

import com.example.urwallet.features.events.data.parser.ExtractorRegistry
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyType
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.repository.ContactResolutionRepository
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import com.example.urwallet.features.people.domain.usecase.ResolvePersonForCounterpartyUseCase
import javax.inject.Inject

class ProcessIncomingSmsUseCase @Inject constructor(
    private val extractorRegistry: ExtractorRegistry,
    private val financialEventRepository: FinancialEventRepository,
    private val contactResolutionRepository: ContactResolutionRepository,
    private val checkDuplicateEventUseCase: CheckDuplicateEventUseCase,
    private val resolvePersonForCounterpartyUseCase: ResolvePersonForCounterpartyUseCase,
    private val suggestCategoryUseCase: SuggestCategoryUseCase
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

        // 3. Resolve Counterparty and Person
        var resolvedCounterparty: Counterparty? = null
        val phone = partial.rawPhoneNumber

        if (!phone.isNullOrBlank()) {
            // 3a. Check Phase 18 Person resolution hierarchy first
            val resolvedPerson = resolvePersonForCounterpartyUseCase(
                phoneNumber = phone,
                counterpartyName = partial.rawCounterparty
            )

            if (resolvedPerson != null) {
                resolvedCounterparty = Counterparty(
                    name = resolvedPerson.name,
                    type = CounterpartyType.PERSON,
                    phoneNumber = phone
                )
            } else {
                // 3b. Check local learned phone mapping
                val learned = financialEventRepository.getMappingForPhone(phone)
                if (learned != null) {
                    resolvedCounterparty = Counterparty(
                        name = learned.name,
                        type = learned.type,
                        phoneNumber = phone
                    )
                } else if (contactResolutionRepository.hasContactsPermission()) {
                    // 3c. Optional fallback to Android Contacts for this specific number
                    val contactName = contactResolutionRepository.resolveContactName(phone)
                    resolvedCounterparty = Counterparty(
                        name = contactName ?: phone,
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
            }
        } else if (!partial.rawCounterparty.isNullOrBlank()) {
            // Check person by name
            val personByName = resolvePersonForCounterpartyUseCase(
                phoneNumber = null,
                counterpartyName = partial.rawCounterparty
            )
            if (personByName != null) {
                resolvedCounterparty = Counterparty(
                    name = personByName.name,
                    type = CounterpartyType.PERSON,
                    phoneNumber = personByName.phoneNumber
                )
            } else {
                resolvedCounterparty = Counterparty(
                    name = partial.rawCounterparty,
                    type = CounterpartyType.MERCHANT,
                    phoneNumber = null
                )
            }
        }

        // 4. Deterministic category suggestion (Tier 1: learned, Tier 2: keywords)
        val categorySuggestion = suggestCategoryUseCase(
            merchantOrCounterparty = resolvedCounterparty?.name ?: partial.rawCounterparty ?: sender,
            rawMessage = partial.rawMessage,
            type = partial.type
        )

        // 5. Check for duplicate or same-event against existing Room transactions
        val matchResult = checkDuplicateEventUseCase(
            amount = partial.amount,
            type = partial.type,
            date = partial.date,
            sourceIdentifier = partial.sourceIdentifier,
            counterpartyName = resolvedCounterparty?.name
        )

        // Drop only if identical source identifier was already processed without an existing transaction
        if (matchResult.status == DuplicateMatchStatus.EXACT_MATCH && matchResult.matchedTransactionId == null) {
            return null
        }

        // 6. Construct domain FinancialEvent
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
            suggestedCategoryId = categorySuggestion?.categoryId,
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

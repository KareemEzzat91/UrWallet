package com.example.urwallet.features.events.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyType
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.model.EventConfidence
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.FinancialEventSource
import com.example.urwallet.features.events.domain.model.InboxStatus

@Entity(
    tableName = "financial_inbox",
    indices = [
        Index(value = ["sourceIdentifier"], unique = true),
        Index(value = ["status"]),
        Index(value = ["date"])
    ]
)
data class FinancialInboxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceType: String,
    val sourceIdentifier: String,
    val rawMessage: String? = null,
    val sender: String,
    val amount: Double,
    val currency: String = "EGP",
    val type: TransactionType,
    val date: Long,
    val accountOrCard: String? = null,
    val counterpartyName: String? = null,
    val counterpartyType: String? = null,
    val phoneNumber: String? = null,
    val suggestedCategoryId: Long? = null,
    val confidence: String,
    val status: String,
    val matchStatus: String,
    val matchedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): FinancialEvent {
        val counterparty = if (!counterpartyName.isNullOrBlank()) {
            Counterparty(
                name = counterpartyName,
                type = runCatching { CounterpartyType.valueOf(counterpartyType ?: "") }.getOrDefault(CounterpartyType.UNKNOWN),
                phoneNumber = phoneNumber
            )
        } else null

        return FinancialEvent(
            id = id,
            sourceType = runCatching { FinancialEventSource.valueOf(sourceType) }.getOrDefault(FinancialEventSource.SMS),
            sourceIdentifier = sourceIdentifier,
            rawMessage = rawMessage,
            sender = sender,
            amount = amount,
            currency = currency,
            type = type,
            date = date,
            accountOrCard = accountOrCard,
            counterparty = counterparty,
            suggestedCategoryId = suggestedCategoryId,
            confidence = runCatching { EventConfidence.valueOf(confidence) }.getOrDefault(EventConfidence.HIGH),
            status = runCatching { InboxStatus.valueOf(status) }.getOrDefault(InboxStatus.PENDING),
            matchStatus = runCatching { DuplicateMatchStatus.valueOf(matchStatus) }.getOrDefault(DuplicateMatchStatus.NEW_EVENT),
            matchedTransactionId = matchedTransactionId,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(event: FinancialEvent): FinancialInboxEntity {
            return FinancialInboxEntity(
                id = event.id,
                sourceType = event.sourceType.name,
                sourceIdentifier = event.sourceIdentifier,
                rawMessage = event.rawMessage,
                sender = event.sender,
                amount = event.amount,
                currency = event.currency,
                type = event.type,
                date = event.date,
                accountOrCard = event.accountOrCard,
                counterpartyName = event.counterparty?.name,
                counterpartyType = event.counterparty?.type?.name,
                phoneNumber = event.counterparty?.phoneNumber,
                suggestedCategoryId = event.suggestedCategoryId,
                confidence = event.confidence.name,
                status = event.status.name,
                matchStatus = event.matchStatus.name,
                matchedTransactionId = event.matchedTransactionId,
                createdAt = event.createdAt
            )
        }
    }
}

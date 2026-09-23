package com.example.urwallet.features.events.domain.model

import com.example.urwallet.core.common.TransactionType

data class FinancialEvent(
    val id: Long = 0,
    val sourceType: FinancialEventSource = FinancialEventSource.SMS,
    val sourceIdentifier: String,
    val rawMessage: String? = null,
    val sender: String,
    val amount: Double,
    val currency: String = "EGP",
    val type: TransactionType,
    val date: Long,
    val accountOrCard: String? = null,
    val counterparty: Counterparty? = null,
    val suggestedCategoryId: Long? = null,
    val confidence: EventConfidence = EventConfidence.HIGH,
    val status: InboxStatus = InboxStatus.PENDING,
    val matchStatus: DuplicateMatchStatus = DuplicateMatchStatus.NEW_EVENT,
    val matchedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

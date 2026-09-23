package com.example.urwallet.features.events.domain.model

import com.example.urwallet.core.common.TransactionType

data class PartialFinancialEvent(
    val amount: Double,
    val type: TransactionType,
    val date: Long,
    val source: FinancialEventSource = FinancialEventSource.SMS,
    val sourceIdentifier: String,
    val rawMessage: String,
    val sender: String,
    val accountOrCard: String? = null,
    val rawCounterparty: String? = null,
    val rawPhoneNumber: String? = null,
    val confidence: EventConfidence = EventConfidence.MEDIUM
)

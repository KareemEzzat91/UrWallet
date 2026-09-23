package com.example.urwallet.features.events.domain.model

import com.example.urwallet.features.people.domain.model.FinancialObligation

data class ObligationSettlementSuggestion(
    val obligation: FinancialObligation,
    val suggestedAmount: Double,
    val matchReason: String,
    val isExactAmountMatch: Boolean = false
)

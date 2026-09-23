package com.example.urwallet.features.people.domain.model

data class FinancialObligation(
    val id: Long = 0,
    val personId: Long,
    val amount: Double,
    val direction: ObligationDirection,
    val reason: String? = null,
    val dueDate: Long? = null,
    val status: ObligationStatus = ObligationStatus.OPEN,
    val settledAmount: Double = 0.0,
    val remainingAmount: Double = amount,
    val relatedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(amount >= 0.0) { "Original obligation amount cannot be negative" }
        require(settledAmount in 0.0..amount) { "Settled amount ($settledAmount) must be between 0 and original amount ($amount)" }
        require(remainingAmount in 0.0..amount) { "Remaining amount ($remainingAmount) must be between 0 and original amount ($amount)" }
    }

    val isSettled: Boolean get() = status == ObligationStatus.SETTLED || remainingAmount <= 0.0001
    val isPartiallySettled: Boolean get() = status == ObligationStatus.PARTIALLY_SETTLED
    val isOpen: Boolean get() = status == ObligationStatus.OPEN
}

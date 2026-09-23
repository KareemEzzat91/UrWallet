package com.example.urwallet.features.people.domain.model

data class ObligationSettlement(
    val id: Long = 0,
    val obligationId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val relatedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(amount > 0.0) { "Settlement amount must be greater than zero" }
    }
}

package com.example.urwallet.features.people.domain.model

data class PersonFinancialSummary(
    val person: Person,
    val totalSent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val transactionCount: Int = 0,
    val lastTransactionDate: Long? = null,
    val totalOwedToMe: Double = 0.0,
    val totalIOwe: Double = 0.0,
    val openObligationsCount: Int = 0
) {
    /**
     * Net money flow from transactions: (Received - Sent).
     * Positive = you received more money from this person than you sent.
     * Negative = you sent more money to this person than you received.
     * NOTE: This does NOT represent debt.
     */
    val netTransactionFlow: Double get() = totalReceived - totalSent

    /**
     * Net financial obligation: (Owed to me - I owe).
     * Positive = This person owes you more than you owe them.
     * Negative = You owe this person more than they owe you.
     */
    val netObligationBalance: Double get() = totalOwedToMe - totalIOwe

    val hasActiveObligations: Boolean get() = totalOwedToMe > 0.001 || totalIOwe > 0.001
}

package com.example.urwallet.features.people.domain.model

data class PeopleAnalyticsSummary(
    val totalPeopleCount: Int = 0,
    val activeRelationshipsCount: Int = 0,
    val totalOwedToMe: Double = 0.0,
    val totalIOwe: Double = 0.0,
    val mostMoneySentTo: PersonFinancialSummary? = null,
    val mostMoneyReceivedFrom: PersonFinancialSummary? = null,
    val mostActivePerson: PersonFinancialSummary? = null,
    val largestDebtOwedToMe: FinancialObligation? = null,
    val largestDebtIOwe: FinancialObligation? = null
) {
    val netObligationBalance: Double get() = totalOwedToMe - totalIOwe
}

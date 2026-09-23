package com.example.urwallet.features.events.domain.model

data class Counterparty(
    val name: String,
    val type: CounterpartyType = CounterpartyType.UNKNOWN,
    val phoneNumber: String? = null
)

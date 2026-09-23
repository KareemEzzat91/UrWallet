package com.example.urwallet.features.events.domain.model

data class CounterpartyMapping(
    val phoneNumber: String,
    val name: String,
    val type: CounterpartyType = CounterpartyType.PERSON,
    val personId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

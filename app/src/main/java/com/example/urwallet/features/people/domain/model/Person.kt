package com.example.urwallet.features.people.domain.model

data class Person(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

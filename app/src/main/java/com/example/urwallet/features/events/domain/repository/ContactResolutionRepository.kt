package com.example.urwallet.features.events.domain.repository

interface ContactResolutionRepository {
    suspend fun resolveContactName(phoneNumber: String): String?
    fun hasContactsPermission(): Boolean
}

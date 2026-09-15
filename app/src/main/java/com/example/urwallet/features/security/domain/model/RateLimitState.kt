package com.example.urwallet.features.security.domain.model

/**
 * Encapsulates the rate limiting and lockout state for PIN entry.
 */
data class RateLimitState(
    val failedAttempts: Int = 0,
    val lockoutUntilTimestamp: Long = 0L,
    val isLockedOut: Boolean = false,
    val remainingSeconds: Long = 0L
) {
    companion object {
        val IDLE = RateLimitState()
    }
}

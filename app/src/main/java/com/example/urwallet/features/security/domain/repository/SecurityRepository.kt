package com.example.urwallet.features.security.domain.repository

import com.example.urwallet.features.security.domain.model.RateLimitState
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for application-level security, PIN credentials,
 * biometrics, and brute-force rate limiting.
 */
interface SecurityRepository {

    val isAppLockEnabled: Flow<Boolean>

    val isBiometricEnabled: Flow<Boolean>

    val rateLimitState: Flow<RateLimitState>

    suspend fun savePin(pin: String): Result<Unit>

    suspend fun verifyPin(candidatePin: String): Boolean

    suspend fun hasPin(): Boolean

    suspend fun clearPin()

    suspend fun setAppLockEnabled(enabled: Boolean)

    suspend fun setBiometricEnabled(enabled: Boolean)

    suspend fun recordFailedAttempt(): RateLimitState

    suspend fun resetRateLimit()

    suspend fun checkRateLimit(): RateLimitState
}

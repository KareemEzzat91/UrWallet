package com.example.urwallet.features.security.data.repository

import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.features.security.data.security.PinCryptoManager
import com.example.urwallet.features.security.domain.model.RateLimitState
import com.example.urwallet.features.security.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val appPreferences: AppPreferences,
    private val pinCryptoManager: PinCryptoManager
) : SecurityRepository {

    override val isAppLockEnabled: Flow<Boolean> = appPreferences.isAppLockEnabled

    override val isBiometricEnabled: Flow<Boolean> = appPreferences.isBiometricEnabled

    override val rateLimitState: Flow<RateLimitState> = combine(
        appPreferences.failedPinAttempts,
        appPreferences.pinLockoutUntil
    ) { attempts, lockoutUntil ->
        val now = System.currentTimeMillis()
        val isLockedOut = lockoutUntil > now
        val remainingSeconds = if (isLockedOut) (lockoutUntil - now + 999) / 1000 else 0L
        RateLimitState(
            failedAttempts = attempts,
            lockoutUntilTimestamp = lockoutUntil,
            isLockedOut = isLockedOut,
            remainingSeconds = remainingSeconds
        )
    }

    override suspend fun savePin(pin: String): Result<Unit> {
        return try {
            val salt = pinCryptoManager.generateSalt()
            val hash = pinCryptoManager.hashPin(pin, salt)
            val saltBase64 = pinCryptoManager.encodeBase64(salt)
            val hashBase64 = pinCryptoManager.encodeBase64(hash)

            // Atomically overwrite credentials in single DataStore edit
            appPreferences.savePin(saltBase64, hashBase64)
            appPreferences.resetFailedPinAttempts()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyPin(candidatePin: String): Boolean {
        // First verify lockout state
        val rateLimit = checkRateLimit()
        if (rateLimit.isLockedOut) {
            return false
        }

        val salt = appPreferences.pinSalt.first() ?: return false
        val hash = appPreferences.pinHash.first() ?: return false

        val isValid = pinCryptoManager.verifyPin(candidatePin, salt, hash)
        if (isValid) {
            resetRateLimit()
        } else {
            recordFailedAttempt()
        }
        return isValid
    }

    override suspend fun hasPin(): Boolean {
        return !appPreferences.pinHash.first().isNullOrBlank()
    }

    override suspend fun clearPin() {
        appPreferences.clearPin()
        appPreferences.resetFailedPinAttempts()
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        appPreferences.setAppLockEnabled(enabled)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        appPreferences.setBiometricEnabled(enabled)
    }

    override suspend fun recordFailedAttempt(): RateLimitState {
        val count = appPreferences.incrementFailedPinAttempts()
        val now = System.currentTimeMillis()
        val delayMs = when {
            count >= LOCKOUT_THRESHOLD_HIGH -> DELAY_HIGH_MS
            count == LOCKOUT_THRESHOLD_MED -> DELAY_MED_MS
            count == LOCKOUT_THRESHOLD_LOW -> DELAY_LOW_MS
            else -> 0L
        }

        val lockoutUntil = if (delayMs > 0L) now + delayMs else 0L
        if (lockoutUntil > 0L) {
            appPreferences.setPinLockoutUntil(lockoutUntil)
        }

        val isLockedOut = delayMs > 0L
        val remainingSeconds = if (isLockedOut) delayMs / 1000 else 0L
        return RateLimitState(
            failedAttempts = count,
            lockoutUntilTimestamp = lockoutUntil,
            isLockedOut = isLockedOut,
            remainingSeconds = remainingSeconds
        )
    }

    override suspend fun resetRateLimit() {
        appPreferences.resetFailedPinAttempts()
    }

    override suspend fun checkRateLimit(): RateLimitState {
        val attempts = appPreferences.failedPinAttempts.first()
        val lockoutUntil = appPreferences.pinLockoutUntil.first()
        val now = System.currentTimeMillis()
        val isLockedOut = lockoutUntil > now
        val remainingSeconds = if (isLockedOut) (lockoutUntil - now + 999) / 1000 else 0L
        return RateLimitState(
            failedAttempts = attempts,
            lockoutUntilTimestamp = lockoutUntil,
            isLockedOut = isLockedOut,
            remainingSeconds = remainingSeconds
        )
    }

    companion object {
        const val LOCKOUT_THRESHOLD_LOW = 5
        const val LOCKOUT_THRESHOLD_MED = 6
        const val LOCKOUT_THRESHOLD_HIGH = 7

        const val DELAY_LOW_MS = 30_000L    // 30 seconds
        const val DELAY_MED_MS = 60_000L    // 1 minute
        const val DELAY_HIGH_MS = 300_000L  // 5 minutes
    }
}

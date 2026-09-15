package com.example.urwallet.features.security.domain.usecase

import com.example.urwallet.features.security.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Safely disables App Lock by requiring verification of the current PIN first.
 */
class DisableAppLockUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    suspend operator fun invoke(currentPin: String): Result<Unit> {
        val isValid = securityRepository.verifyPin(currentPin)
        if (!isValid) {
            return Result.failure(IllegalArgumentException("رمز PIN الحالي غير صحيح"))
        }
        securityRepository.clearPin()
        return Result.success(Unit)
    }
}

package com.example.urwallet.features.security.domain.usecase

import com.example.urwallet.features.security.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Executes a safe, atomic PIN change:
 * 1. Verifies the current PIN.
 * 2. Validates the new PIN format.
 * 3. Atomically generates fresh credentials (salt + hash) and persists them.
 * 4. Ensures the old PIN remains 100% active and untouched if any verification or validation fails.
 */
class ChangePinUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    suspend operator fun invoke(currentPin: String, newPin: String): Result<Unit> {
        val isCurrentValid = securityRepository.verifyPin(currentPin)
        if (!isCurrentValid) {
            return Result.failure(IllegalArgumentException("رمز PIN الحالي غير صحيح"))
        }

        if (newPin.length != SetPinUseCase.PIN_LENGTH || !newPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("يجب أن يتكون رمز PIN الجديد من 4 أرقام فقط"))
        }

        // Atomically overwrites only upon successful verification and validation
        return securityRepository.savePin(newPin)
    }
}

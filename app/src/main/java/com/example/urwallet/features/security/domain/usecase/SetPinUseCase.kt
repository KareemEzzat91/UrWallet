package com.example.urwallet.features.security.domain.usecase

import com.example.urwallet.features.security.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Validates and sets a new 4-digit PIN for the application.
 */
class SetPinUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    suspend operator fun invoke(pin: String): Result<Unit> {
        if (pin.length != PIN_LENGTH || !pin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("يجب أن يتكون رمز PIN من 4 أرقام فقط"))
        }
        return securityRepository.savePin(pin)
    }

    companion object {
        const val PIN_LENGTH = 4
    }
}

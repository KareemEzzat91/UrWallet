package com.example.urwallet.features.security.domain.usecase

import com.example.urwallet.features.security.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Verifies a candidate 4-digit PIN against stored credentials, handling rate limiting.
 */
class VerifyPinUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    suspend operator fun invoke(candidatePin: String): Boolean {
        if (candidatePin.length != SetPinUseCase.PIN_LENGTH || !candidatePin.all { it.isDigit() }) {
            return false
        }
        return securityRepository.verifyPin(candidatePin)
    }
}

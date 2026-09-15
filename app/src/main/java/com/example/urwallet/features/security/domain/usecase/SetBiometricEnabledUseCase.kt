package com.example.urwallet.features.security.domain.usecase

import com.example.urwallet.features.security.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Updates the user's preference for biometric authentication.
 */
class SetBiometricEnabledUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        securityRepository.setBiometricEnabled(enabled)
    }
}

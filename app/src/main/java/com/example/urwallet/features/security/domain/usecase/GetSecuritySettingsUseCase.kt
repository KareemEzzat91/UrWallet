package com.example.urwallet.features.security.domain.usecase

import com.example.urwallet.features.security.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class SecuritySettings(
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val hasPin: Boolean = false
)

/**
 * Consolidates security settings into a single Flow.
 */
class GetSecuritySettingsUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(): Flow<SecuritySettings> {
        return combine(
            securityRepository.isAppLockEnabled,
            securityRepository.isBiometricEnabled
        ) { isLockEnabled, isBioEnabled ->
            SecuritySettings(
                isAppLockEnabled = isLockEnabled,
                isBiometricEnabled = isBioEnabled,
                hasPin = isLockEnabled // In our model, enabling lock creates a valid PIN
            )
        }
    }
}

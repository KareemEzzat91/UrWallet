package com.example.urwallet.features.security.presentation.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.security.domain.model.RateLimitState
import com.example.urwallet.features.security.domain.repository.SecurityRepository
import com.example.urwallet.features.security.domain.session.AppLockManager
import com.example.urwallet.features.security.domain.usecase.VerifyPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppLockUiState(
    val pinLength: Int = 0,
    val isVerifying: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val rateLimitState: RateLimitState = RateLimitState.IDLE,
    val isBiometricEnabled: Boolean = false,
    val shouldTriggerBiometric: Boolean = false
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val verifyPinUseCase: VerifyPinUseCase,
    private val securityRepository: SecurityRepository,
    private val appLockManager: AppLockManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    private val pinBuffer = StringBuilder()
    private var countdownJob: Job? = null

    init {
        observeRateLimit()
        checkInitialBiometric()
    }

    private fun checkInitialBiometric() {
        viewModelScope.launch {
            val isBioEnabled = securityRepository.isBiometricEnabled.first()
            _uiState.update {
                it.copy(
                    isBiometricEnabled = isBioEnabled,
                    shouldTriggerBiometric = isBioEnabled
                )
            }
        }
    }

    private fun observeRateLimit() {
        viewModelScope.launch {
            securityRepository.rateLimitState.collect { rateLimit ->
                _uiState.update { it.copy(rateLimitState = rateLimit) }
                if (rateLimit.isLockedOut) {
                    startCountdownTimer(rateLimit.remainingSeconds)
                } else {
                    countdownJob?.cancel()
                }
            }
        }
    }

    private fun startCountdownTimer(initialSeconds: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = initialSeconds
            while (remaining > 0) {
                _uiState.update {
                    it.copy(rateLimitState = it.rateLimitState.copy(remainingSeconds = remaining, isLockedOut = true))
                }
                delay(1000L)
                remaining--
            }
            _uiState.update {
                it.copy(rateLimitState = it.rateLimitState.copy(remainingSeconds = 0, isLockedOut = false), errorMessage = null)
            }
        }
    }

    fun appendDigit(digit: Char) {
        if (_uiState.value.rateLimitState.isLockedOut || _uiState.value.isVerifying) return
        if (pinBuffer.length >= 4) return

        pinBuffer.append(digit)
        _uiState.update { it.copy(pinLength = pinBuffer.length, errorMessage = null) }

        if (pinBuffer.length == 4) {
            verifyCurrentPin()
        }
    }

    fun deleteDigit() {
        if (_uiState.value.rateLimitState.isLockedOut || _uiState.value.isVerifying) return
        if (pinBuffer.isNotEmpty()) {
            pinBuffer.deleteCharAt(pinBuffer.length - 1)
            _uiState.update { it.copy(pinLength = pinBuffer.length, errorMessage = null) }
        }
    }

    fun clearPin() {
        pinBuffer.clear()
        _uiState.update { it.copy(pinLength = 0) }
    }

    private fun verifyCurrentPin() {
        val candidate = pinBuffer.toString()
        _uiState.update { it.copy(isVerifying = true) }

        viewModelScope.launch {
            val isValid = verifyPinUseCase(candidate)
            pinBuffer.clear()

            if (isValid) {
                appLockManager.unlock()
                _uiState.update {
                    it.copy(
                        pinLength = 0,
                        isVerifying = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }
            } else {
                val currentRateLimit = securityRepository.checkRateLimit()
                _uiState.update {
                    it.copy(
                        pinLength = 0,
                        isVerifying = false,
                        isSuccess = false,
                        errorMessage = if (currentRateLimit.isLockedOut) null else "رمز PIN غير صحيح. حاول مرة أخرى",
                        rateLimitState = currentRateLimit
                    )
                }
                if (currentRateLimit.isLockedOut) {
                    startCountdownTimer(currentRateLimit.remainingSeconds)
                }
            }
        }
    }

    fun onBiometricSuccess() {
        appLockManager.unlock()
        viewModelScope.launch {
            securityRepository.resetRateLimit()
        }
        _uiState.update { it.copy(isSuccess = true, shouldTriggerBiometric = false) }
    }

    fun onBiometricDismissed() {
        _uiState.update { it.copy(shouldTriggerBiometric = false) }
    }

    fun triggerBiometricPrompt() {
        if (_uiState.value.isBiometricEnabled && !_uiState.value.rateLimitState.isLockedOut) {
            _uiState.update { it.copy(shouldTriggerBiometric = true) }
        }
    }
}

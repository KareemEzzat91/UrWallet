package com.example.urwallet.features.security.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.security.domain.usecase.ChangePinUseCase
import com.example.urwallet.features.security.domain.usecase.DisableAppLockUseCase
import com.example.urwallet.features.security.domain.usecase.GetSecuritySettingsUseCase
import com.example.urwallet.features.security.domain.usecase.SecuritySettings
import com.example.urwallet.features.security.domain.usecase.SetBiometricEnabledUseCase
import com.example.urwallet.features.security.domain.usecase.SetPinUseCase
import com.example.urwallet.features.security.domain.usecase.VerifyPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SecurityEvent {
    data class ShowSnackbar(val message: String) : SecurityEvent
    data object PinSetupSuccess : SecurityEvent
    data object PinChangeSuccess : SecurityEvent
    data object AppLockDisabled : SecurityEvent
}

enum class PinWizardMode {
    SETUP,
    CHANGE
}

enum class PinWizardStep {
    VERIFY_CURRENT, // Only in CHANGE mode
    ENTER_NEW,
    CONFIRM_NEW,
    BIOMETRICS,
    SUCCESS
}

data class PinWizardUiState(
    val mode: PinWizardMode = PinWizardMode.SETUP,
    val step: PinWizardStep = PinWizardStep.ENTER_NEW,
    val pinLength: Int = 0,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isBiometricToggleChecked: Boolean = false
)

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val getSecuritySettingsUseCase: GetSecuritySettingsUseCase,
    private val setPinUseCase: SetPinUseCase,
    private val changePinUseCase: ChangePinUseCase,
    private val disableAppLockUseCase: DisableAppLockUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val verifyPinUseCase: VerifyPinUseCase
) : ViewModel() {

    private val _settingsState = MutableStateFlow(SecuritySettings())
    val settingsState: StateFlow<SecuritySettings> = _settingsState.asStateFlow()

    private val _wizardUiState = MutableStateFlow(PinWizardUiState())
    val wizardUiState: StateFlow<PinWizardUiState> = _wizardUiState.asStateFlow()

    private val _events = MutableSharedFlow<SecurityEvent>()
    val events: SharedFlow<SecurityEvent> = _events.asSharedFlow()

    private val currentPinBuffer = StringBuilder()
    private var verifiedCurrentPin: String? = null
    private var firstEnteredNewPin: String? = null

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getSecuritySettingsUseCase().collect { settings ->
                _settingsState.value = settings
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            setBiometricEnabledUseCase(enabled)
        }
    }

    fun disableAppLock(currentPin: String, onResult: (success: Boolean, errorMsg: String?) -> Unit) {
        viewModelScope.launch {
            val result = disableAppLockUseCase(currentPin)
            result.fold(
                onSuccess = {
                    _events.emit(SecurityEvent.AppLockDisabled)
                    onResult(true, null)
                },
                onFailure = { error ->
                    onResult(false, error.message ?: "رمز PIN غير صحيح")
                }
            )
        }
    }

    // --- Wizard Methods ---

    fun startPinSetup() {
        currentPinBuffer.clear()
        verifiedCurrentPin = null
        firstEnteredNewPin = null
        _wizardUiState.value = PinWizardUiState(
            mode = PinWizardMode.SETUP,
            step = PinWizardStep.ENTER_NEW,
            pinLength = 0
        )
    }

    fun startPinChange() {
        currentPinBuffer.clear()
        verifiedCurrentPin = null
        firstEnteredNewPin = null
        _wizardUiState.value = PinWizardUiState(
            mode = PinWizardMode.CHANGE,
            step = PinWizardStep.VERIFY_CURRENT,
            pinLength = 0
        )
    }

    fun appendWizardDigit(digit: Char) {
        if (_wizardUiState.value.isLoading || currentPinBuffer.length >= 4) return

        currentPinBuffer.append(digit)
        _wizardUiState.update { it.copy(pinLength = currentPinBuffer.length, errorMessage = null) }

        if (currentPinBuffer.length == 4) {
            handleWizardPinSubmission()
        }
    }

    fun deleteWizardDigit() {
        if (_wizardUiState.value.isLoading || currentPinBuffer.isEmpty()) return
        currentPinBuffer.deleteCharAt(currentPinBuffer.length - 1)
        _wizardUiState.update { it.copy(pinLength = currentPinBuffer.length, errorMessage = null) }
    }

    private fun handleWizardPinSubmission() {
        val entered = currentPinBuffer.toString()
        val currentState = _wizardUiState.value

        when (currentState.step) {
            PinWizardStep.VERIFY_CURRENT -> {
                _wizardUiState.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    val isValid = verifyPinUseCase(entered)
                    currentPinBuffer.clear()
                    if (isValid) {
                        verifiedCurrentPin = entered
                        _wizardUiState.update {
                            it.copy(
                                step = PinWizardStep.ENTER_NEW,
                                pinLength = 0,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    } else {
                        _wizardUiState.update {
                            it.copy(
                                pinLength = 0,
                                isLoading = false,
                                errorMessage = "رمز PIN الحالي غير صحيح"
                            )
                        }
                    }
                }
            }

            PinWizardStep.ENTER_NEW -> {
                firstEnteredNewPin = entered
                currentPinBuffer.clear()
                _wizardUiState.update {
                    it.copy(
                        step = PinWizardStep.CONFIRM_NEW,
                        pinLength = 0,
                        errorMessage = null
                    )
                }
            }

            PinWizardStep.CONFIRM_NEW -> {
                val expected = firstEnteredNewPin
                if (entered == expected) {
                    executePinSave(entered)
                } else {
                    currentPinBuffer.clear()
                    _wizardUiState.update {
                        it.copy(
                            pinLength = 0,
                            errorMessage = "الرمزان غير متطابقين، يرجى المحاولة مرة أخرى"
                        )
                    }
                }
            }

            else -> { /* No op */ }
        }
    }

    private fun executePinSave(newPin: String) {
        _wizardUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = if (_wizardUiState.value.mode == PinWizardMode.CHANGE) {
                val current = verifiedCurrentPin ?: ""
                changePinUseCase(current, newPin)
            } else {
                setPinUseCase(newPin)
            }

            currentPinBuffer.clear()
            result.fold(
                onSuccess = {
                    if (_wizardUiState.value.mode == PinWizardMode.SETUP) {
                        _wizardUiState.update {
                            it.copy(
                                step = PinWizardStep.BIOMETRICS,
                                pinLength = 0,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    } else {
                        _wizardUiState.update {
                            it.copy(
                                step = PinWizardStep.SUCCESS,
                                pinLength = 0,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                        _events.emit(SecurityEvent.PinChangeSuccess)
                    }
                },
                onFailure = { error ->
                    _wizardUiState.update {
                        it.copy(
                            pinLength = 0,
                            isLoading = false,
                            errorMessage = error.message ?: "فشل حفظ رمز PIN"
                        )
                    }
                }
            )
        }
    }

    fun completeWizard(enableBiometric: Boolean) {
        viewModelScope.launch {
            if (enableBiometric) {
                setBiometricEnabledUseCase(true)
            }
            _wizardUiState.update { it.copy(step = PinWizardStep.SUCCESS) }
            _events.emit(SecurityEvent.PinSetupSuccess)
        }
    }
}

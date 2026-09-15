package com.example.urwallet.features.security

import com.example.urwallet.features.security.domain.model.RateLimitState
import com.example.urwallet.features.security.domain.repository.SecurityRepository
import com.example.urwallet.features.security.domain.usecase.ChangePinUseCase
import com.example.urwallet.features.security.domain.usecase.DisableAppLockUseCase
import com.example.urwallet.features.security.domain.usecase.GetSecuritySettingsUseCase
import com.example.urwallet.features.security.domain.usecase.SetPinUseCase
import com.example.urwallet.features.security.domain.usecase.VerifyPinUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSecurityRepository : SecurityRepository {
    var storedPin: String? = null
    val appLockEnabledFlow = MutableStateFlow(false)
    val biometricEnabledFlow = MutableStateFlow(false)
    val rateLimitFlow = MutableStateFlow(RateLimitState.IDLE)

    override val isAppLockEnabled: Flow<Boolean> = appLockEnabledFlow
    override val isBiometricEnabled: Flow<Boolean> = biometricEnabledFlow
    override val rateLimitState: Flow<RateLimitState> = rateLimitFlow

    override suspend fun savePin(pin: String): Result<Unit> {
        storedPin = pin
        appLockEnabledFlow.value = true
        return Result.success(Unit)
    }

    override suspend fun verifyPin(candidatePin: String): Boolean {
        return candidatePin == storedPin
    }

    override suspend fun hasPin(): Boolean {
        return storedPin != null
    }

    override suspend fun clearPin() {
        storedPin = null
        appLockEnabledFlow.value = false
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        appLockEnabledFlow.value = enabled
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        biometricEnabledFlow.value = enabled
    }

    override suspend fun recordFailedAttempt(): RateLimitState {
        val current = rateLimitFlow.value
        val newAttempts = current.failedAttempts + 1
        val isLockedOut = newAttempts >= 5
        val updated = RateLimitState(
            failedAttempts = newAttempts,
            lockoutUntilTimestamp = if (isLockedOut) System.currentTimeMillis() + 30000L else 0L,
            isLockedOut = isLockedOut,
            remainingSeconds = if (isLockedOut) 30L else 0L
        )
        rateLimitFlow.value = updated
        return updated
    }

    override suspend fun resetRateLimit() {
        rateLimitFlow.value = RateLimitState.IDLE
    }

    override suspend fun checkRateLimit(): RateLimitState {
        return rateLimitFlow.value
    }
}

class SecurityUseCasesTest {

    private lateinit var fakeRepository: FakeSecurityRepository
    private lateinit var setPinUseCase: SetPinUseCase
    private lateinit var verifyPinUseCase: VerifyPinUseCase
    private lateinit var changePinUseCase: ChangePinUseCase
    private lateinit var disableAppLockUseCase: DisableAppLockUseCase
    private lateinit var getSecuritySettingsUseCase: GetSecuritySettingsUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSecurityRepository()
        setPinUseCase = SetPinUseCase(fakeRepository)
        verifyPinUseCase = VerifyPinUseCase(fakeRepository)
        changePinUseCase = ChangePinUseCase(fakeRepository)
        disableAppLockUseCase = DisableAppLockUseCase(fakeRepository)
        getSecuritySettingsUseCase = GetSecuritySettingsUseCase(fakeRepository)
    }

    @Test
    fun setPinUseCase_valid4Digits_savesPinSuccessfully() = runTest {
        val result = setPinUseCase("1234")

        assertTrue(result.isSuccess)
        assertEquals("1234", fakeRepository.storedPin)
        assertTrue(fakeRepository.appLockEnabledFlow.value)
    }

    @Test
    fun setPinUseCase_lessThan4Digits_failsValidation() = runTest {
        val result = setPinUseCase("12")

        assertTrue(result.isFailure)
        assertEquals(null, fakeRepository.storedPin)
    }

    @Test
    fun setPinUseCase_moreThan4Digits_failsValidation() = runTest {
        val result = setPinUseCase("12345")

        assertTrue(result.isFailure)
        assertEquals(null, fakeRepository.storedPin)
    }

    @Test
    fun setPinUseCase_nonNumeric_failsValidation() = runTest {
        val result = setPinUseCase("12ab")

        assertTrue(result.isFailure)
        assertEquals(null, fakeRepository.storedPin)
    }

    @Test
    fun verifyPinUseCase_correctPin_returnsTrue() = runTest {
        fakeRepository.savePin("9876")

        val result = verifyPinUseCase("9876")
        assertTrue(result)
    }

    @Test
    fun verifyPinUseCase_wrongPin_returnsFalse() = runTest {
        fakeRepository.savePin("9876")

        val result = verifyPinUseCase("1111")
        assertFalse(result)
    }

    @Test
    fun verifyPinUseCase_invalidFormat_returnsFalseImmediately() = runTest {
        fakeRepository.savePin("9876")

        assertFalse(verifyPinUseCase("98"))
        assertFalse(verifyPinUseCase("98765"))
        assertFalse(verifyPinUseCase("abcd"))
    }

    @Test
    fun changePinUseCase_validCurrentAndNew_updatesPinAtomically() = runTest {
        fakeRepository.savePin("1111")

        val result = changePinUseCase("1111", "2222")

        assertTrue(result.isSuccess)
        assertEquals("2222", fakeRepository.storedPin)
    }

    @Test
    fun changePinUseCase_wrongCurrentPin_failsAndLeavesOldPinActive() = runTest {
        fakeRepository.savePin("1111")

        val result = changePinUseCase("9999", "2222")

        assertTrue(result.isFailure)
        assertEquals("1111", fakeRepository.storedPin) // Old PIN untouched!
    }

    @Test
    fun changePinUseCase_invalidNewPin_failsAndLeavesOldPinActive() = runTest {
        fakeRepository.savePin("1111")

        val result = changePinUseCase("1111", "22")

        assertTrue(result.isFailure)
        assertEquals("1111", fakeRepository.storedPin) // Old PIN untouched!
    }

    @Test
    fun disableAppLockUseCase_correctCurrentPin_clearsCredentials() = runTest {
        fakeRepository.savePin("1234")

        val result = disableAppLockUseCase("1234")

        assertTrue(result.isSuccess)
        assertEquals(null, fakeRepository.storedPin)
        assertFalse(fakeRepository.appLockEnabledFlow.value)
    }

    @Test
    fun disableAppLockUseCase_wrongCurrentPin_failsAndPreservesLock() = runTest {
        fakeRepository.savePin("1234")

        val result = disableAppLockUseCase("0000")

        assertTrue(result.isFailure)
        assertEquals("1234", fakeRepository.storedPin)
        assertTrue(fakeRepository.appLockEnabledFlow.value)
    }

    @Test
    fun getSecuritySettingsUseCase_combinesFlowsProperly() = runTest {
        fakeRepository.appLockEnabledFlow.value = true
        fakeRepository.biometricEnabledFlow.value = true

        val settings = getSecuritySettingsUseCase().first()

        assertTrue(settings.isAppLockEnabled)
        assertTrue(settings.isBiometricEnabled)
        assertTrue(settings.hasPin)
    }
}

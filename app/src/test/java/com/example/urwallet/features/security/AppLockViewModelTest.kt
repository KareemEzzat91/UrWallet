package com.example.urwallet.features.security

import com.example.urwallet.features.security.domain.model.RateLimitState
import com.example.urwallet.features.security.domain.session.AppLockManager
import com.example.urwallet.features.security.domain.usecase.VerifyPinUseCase
import com.example.urwallet.features.security.presentation.lock.AppLockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSecurityRepository
    private lateinit var appLockManager: AppLockManager
    private lateinit var verifyPinUseCase: VerifyPinUseCase
    private lateinit var viewModel: AppLockViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSecurityRepository()
        fakeRepository.appLockEnabledFlow.value = true
        appLockManager = AppLockManager(fakeRepository)
        appLockManager.lock()
        verifyPinUseCase = VerifyPinUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AppLockViewModel {
        return AppLockViewModel(
            verifyPinUseCase = verifyPinUseCase,
            securityRepository = fakeRepository,
            appLockManager = appLockManager
        )
    }

    @Test
    fun initialUiState_isIdle() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.pinLength)
        assertFalse(state.isVerifying)
        assertFalse(state.isSuccess)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun appendAndBackSpace_updatesPinLengthCorrectly() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.appendDigit('1')
        viewModel.appendDigit('2')
        assertEquals(2, viewModel.uiState.value.pinLength)

        viewModel.deleteDigit()
        assertEquals(1, viewModel.uiState.value.pinLength)

        viewModel.clearPin()
        assertEquals(0, viewModel.uiState.value.pinLength)
    }

    @Test
    fun appendFourthDigit_whenPinIsCorrect_triggersUnlockAndEmitsSuccess() = runTest {
        fakeRepository.savePin("1234")
        appLockManager.lock()
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.appendDigit('1')
        viewModel.appendDigit('2')
        viewModel.appendDigit('3')
        viewModel.appendDigit('4')
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isVerifying)
        assertEquals(0, state.pinLength)
        assertTrue(appLockManager.isUnlocked)
    }

    @Test
    fun appendFourthDigit_whenPinIsWrong_emitsErrorAndClearsPin() = runTest {
        fakeRepository.savePin("1234")
        appLockManager.lock()
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.appendDigit('0')
        viewModel.appendDigit('0')
        viewModel.appendDigit('0')
        viewModel.appendDigit('0')
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.pinLength)
        assertFalse(appLockManager.isUnlocked)
    }

    @Test
    fun onBiometricSuccess_unlocksSessionAndEmitsSuccess() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onBiometricSuccess()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertTrue(appLockManager.isUnlocked)
    }

    @Test
    fun whenLockedOut_cannotAppendDigits() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        fakeRepository.rateLimitFlow.value = RateLimitState(
            failedAttempts = 5,
            lockoutUntilTimestamp = System.currentTimeMillis() + 30000L,
            isLockedOut = true,
            remainingSeconds = 30L
        )
        testDispatcher.scheduler.runCurrent()

        viewModel.appendDigit('1')
        assertEquals(0, viewModel.uiState.value.pinLength)
    }
}

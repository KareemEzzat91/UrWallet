package com.example.urwallet.features.security

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.urwallet.features.security.domain.session.AppLockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeLifecycleOwner : LifecycleOwner {
    override val lifecycle: Lifecycle
        get() = throw UnsupportedOperationException("Not needed in unit test")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSecurityRepository
    private lateinit var appLockManager: AppLockManager
    private val fakeLifecycleOwner = FakeLifecycleOwner()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSecurityRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun coldStart_whenAppLockEnabled_requiresAuthentication() = runTest {
        fakeRepository.appLockEnabledFlow.value = true
        appLockManager = AppLockManager(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(appLockManager.isUnlocked)
        assertTrue(appLockManager.shouldLockOnForeground())
    }

    @Test
    fun coldStart_whenAppLockDisabled_doesNotRequireAuthentication() = runTest {
        fakeRepository.appLockEnabledFlow.value = false
        appLockManager = AppLockManager(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(appLockManager.isUnlocked)
        assertFalse(appLockManager.shouldLockOnForeground())
    }

    @Test
    fun unlock_updatesSessionStateCorrectly() = runTest {
        fakeRepository.appLockEnabledFlow.value = true
        appLockManager = AppLockManager(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        appLockManager.unlock()

        assertTrue(appLockManager.isUnlocked)
        assertFalse(appLockManager.shouldLockOnForeground())
    }

    @Test
    fun backgrounding_thenReturningToForeground_locksSession() = runTest {
        fakeRepository.appLockEnabledFlow.value = true
        appLockManager = AppLockManager(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // User logs in successfully
        appLockManager.unlock()
        assertTrue(appLockManager.isUnlocked)

        // App goes to background
        appLockManager.onStop(fakeLifecycleOwner)
        assertTrue(appLockManager.isBackgrounded)

        // App returns to foreground
        appLockManager.onStart(fakeLifecycleOwner)
        assertFalse(appLockManager.isUnlocked)
        assertTrue(appLockManager.shouldLockOnForeground())
    }

    @Test
    fun foregrounding_withoutBackgrounding_doesNotRelockSession() = runTest {
        fakeRepository.appLockEnabledFlow.value = true
        appLockManager = AppLockManager(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        appLockManager.unlock()
        assertTrue(appLockManager.isUnlocked)

        // Simulated Activity recreation / transition where process ON_STOP did not fire
        appLockManager.onStart(fakeLifecycleOwner)
        assertTrue(appLockManager.isUnlocked)
        assertFalse(appLockManager.shouldLockOnForeground())
    }

    @Test
    fun duplicateLaunch_preventedByLockScreenShowingGuard() = runTest {
        fakeRepository.appLockEnabledFlow.value = true
        appLockManager = AppLockManager(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(appLockManager.shouldLockOnForeground())

        // AppLockActivity launched
        appLockManager.setLockScreenShowing(true)
        assertFalse(appLockManager.shouldLockOnForeground()) // Guard prevents duplicate!

        // Dismissed / Unlocked
        appLockManager.unlock()
        assertFalse(appLockManager.isLockScreenShowing)
        assertFalse(appLockManager.shouldLockOnForeground())
    }

    @Test
    fun disablingLock_immediatelyUnlocksSession() = runTest {
        fakeRepository.appLockEnabledFlow.value = true
        appLockManager = AppLockManager(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(appLockManager.isUnlocked)

        fakeRepository.appLockEnabledFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(appLockManager.isUnlocked)
        assertFalse(appLockManager.shouldLockOnForeground())
    }
}

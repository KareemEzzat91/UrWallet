package com.example.urwallet.features.security.domain.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.urwallet.features.security.domain.repository.SecurityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton managing runtime session authentication and lifecycle lock state.
 *
 * Implements [DefaultLifecycleObserver] registered with [androidx.lifecycle.ProcessLifecycleOwner].
 *
 * Rules:
 * 1. [isUnlocked] is STRICTLY an in-memory session flag. It is NEVER persisted.
 * 2. Activity recreation (e.g. rotation) does NOT trigger ProcessLifecycleOwner ON_STOP.
 * 3. Moving to background sets [isBackgrounded] = true in ON_STOP.
 * 4. Returning to foreground in ON_START resets [isUnlocked] = false if App Lock is enabled.
 * 5. [isLockScreenShowing] prevents duplicate launches of [AppLockActivity].
 */
@Singleton
class AppLockManager @Inject constructor(
    private val securityRepository: SecurityRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var isAppLockEnabledCached: Boolean = false

    @Volatile
    var isUnlocked: Boolean = false
        private set

    @Volatile
    var isLockScreenShowing: Boolean = false
        private set

    @Volatile
    var isBackgrounded: Boolean = false
        private set

    init {
        scope.launch {
            securityRepository.isAppLockEnabled.collect { enabled ->
                isAppLockEnabledCached = enabled
                if (!enabled) {
                    // If disabled by user, session is automatically unlocked
                    isUnlocked = true
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // App entered background
        markBackgrounded()
    }

    override fun onStart(owner: LifecycleOwner) {
        // App returned to foreground
        if (isAppLockEnabledCached && isBackgrounded) {
            isUnlocked = false
        }
        isBackgrounded = false
    }

    fun markBackgrounded() {
        isBackgrounded = true
    }

    fun shouldLockOnForeground(): Boolean {
        return isAppLockEnabledCached && !isUnlocked && !isLockScreenShowing
    }

    suspend fun shouldLockSuspend(): Boolean {
        val enabled = securityRepository.isAppLockEnabled.first()
        isAppLockEnabledCached = enabled
        if (!enabled) {
            isUnlocked = true
        }
        return enabled && !isUnlocked && !isLockScreenShowing
    }

    fun setLockScreenShowing(showing: Boolean) {
        isLockScreenShowing = showing
    }

    fun unlock() {
        isUnlocked = true
        isLockScreenShowing = false
        isBackgrounded = false
    }

    fun lock() {
        isUnlocked = false
    }

    /**
     * Synchronously syncs lock state on application cold start.
     */
    fun syncLockStateBlocking() {
        try {
            isAppLockEnabledCached = runBlocking { securityRepository.isAppLockEnabled.first() }
            if (!isAppLockEnabledCached) {
                isUnlocked = true
            }
        } catch (_: Exception) {
        }
    }
}

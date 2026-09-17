package com.example.urwallet.features.security.presentation.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.urwallet.features.security.domain.session.AppLockManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android lifecycle observer that bridges ProcessLifecycleOwner events to the pure-domain [AppLockManager].
 */
@Singleton
class AppLockLifecycleObserver @Inject constructor(
    private val appLockManager: AppLockManager
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        appLockManager.onAppForegrounded()
    }

    override fun onStop(owner: LifecycleOwner) {
        appLockManager.onAppBackgrounded()
    }
}

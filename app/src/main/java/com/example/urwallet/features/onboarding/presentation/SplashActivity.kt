package com.example.urwallet.features.onboarding.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.urwallet.MainActivity
import com.example.urwallet.core.datastore.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var appLockManager: com.example.urwallet.features.security.domain.session.AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash visible while we check onboarding & security state
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        lifecycleScope.launch {
            val onboardingCompleted = appPreferences.isOnboardingCompleted.first()
            keepSplash = false

            val intent = if (!onboardingCompleted) {
                Intent(this@SplashActivity, OnboardingActivity::class.java)
            } else if (appLockManager.shouldLockSuspend()) {
                Intent(this@SplashActivity, com.example.urwallet.features.security.presentation.lock.AppLockActivity::class.java).apply {
                    putExtra(com.example.urwallet.features.security.presentation.lock.AppLockActivity.EXTRA_NAVIGATE_TO_MAIN, true)
                }
            } else {
                Intent(this@SplashActivity, MainActivity::class.java)
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}

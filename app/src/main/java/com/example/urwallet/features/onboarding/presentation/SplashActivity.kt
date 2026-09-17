package com.example.urwallet.features.onboarding.presentation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.urwallet.MainActivity
import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.databinding.ActivitySplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

    private lateinit var binding: ActivitySplashBinding
    private var pulseAnimatorX: ObjectAnimator? = null
    private var pulseAnimatorY: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Allow system splash to instantly transition into custom animated layout
        splashScreen.setKeepOnScreenCondition { false }

        setupInitialAnimationState()
        startEntranceAnimations()

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            val onboardingCompleted = appPreferences.isOnboardingCompleted.first()
            val shouldLock = appLockManager.shouldLockSuspend()

            // Smooth minimum splash duration to appreciate brand animation
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = (1600L - elapsed).coerceAtLeast(0L)
            if (remaining > 0) {
                delay(remaining)
            }

            val intent = if (!onboardingCompleted) {
                Intent(this@SplashActivity, OnboardingActivity::class.java)
            } else if (shouldLock) {
                Intent(this@SplashActivity, com.example.urwallet.features.security.presentation.lock.AppLockActivity::class.java).apply {
                    putExtra(com.example.urwallet.features.security.presentation.lock.AppLockActivity.EXTRA_NAVIGATE_TO_MAIN, true)
                }
            } else {
                Intent(this@SplashActivity, MainActivity::class.java)
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

            finish()
        }
    }

    private fun setupInitialAnimationState() {
        binding.viewAura.alpha = 0f
        binding.frameLogoContainer.scaleX = 0.5f
        binding.frameLogoContainer.scaleY = 0.5f
        binding.frameLogoContainer.alpha = 0f
        binding.tvAppName.alpha = 0f
        binding.tvAppName.translationY = 35f
        binding.tvAppEnglish.alpha = 0f
        binding.tvAppTagline.alpha = 0f
        binding.tvAppTagline.translationY = 25f
        binding.layoutFooter.alpha = 0f
    }

    private fun startEntranceAnimations() {
        // 1. Ambient Background Aura Fade In
        binding.viewAura.animate()
            .alpha(1f)
            .setDuration(800)
            .start()

        // 2. Logo Hero Pop with Elastic Overshoot
        binding.frameLogoContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(700)
            .setInterpolator(OvershootInterpolator(1.25f))
            .withEndAction {
                startBreathingPulse(binding.frameLogoContainer)
            }
            .start()

        // 3. Arabic App Name Slide Up & Glow
        binding.tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(550)
            .setStartDelay(200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 4. English Brand Identifier Fade In
        binding.tvAppEnglish.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(350)
            .start()

        // 5. App Tagline Slide Up
        binding.tvAppTagline.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(550)
            .setStartDelay(450)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 6. Footer Indicator Fade In
        binding.layoutFooter.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(600)
            .start()
    }

    private fun startBreathingPulse(view: View) {
        pulseAnimatorX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.04f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        pulseAnimatorY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.04f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnimatorX?.cancel()
        pulseAnimatorY?.cancel()
    }
}

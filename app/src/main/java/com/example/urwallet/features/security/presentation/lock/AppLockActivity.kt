package com.example.urwallet.features.security.presentation.lock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.urwallet.R
import com.example.urwallet.databinding.ActivityAppLockBinding
import com.example.urwallet.features.security.domain.session.AppLockManager
import com.example.urwallet.features.security.presentation.biometrics.BiometricAuthManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppLockBinding
    private val viewModel: AppLockViewModel by viewModels()

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private val dots by lazy {
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce anti-screenshot & recent-apps protection
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)

        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appLockManager.setLockScreenShowing(true)

        setupBackPressHandler()
        setupKeypad()
        observeState()
    }

    private fun setupBackPressHandler() {
        // Prevent bypassing lock screen: pressing back minimizes the app to background
        onBackPressedDispatcher.addCallback(this) {
            moveTaskToBack(true)
        }
    }

    private fun setupKeypad() {
        val numberButtons = listOf(
            binding.btn0 to '0',
            binding.btn1 to '1',
            binding.btn2 to '2',
            binding.btn3 to '3',
            binding.btn4 to '4',
            binding.btn5 to '5',
            binding.btn6 to '6',
            binding.btn7 to '7',
            binding.btn8 to '8',
            binding.btn9 to '9'
        )

        numberButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.appendDigit(digit)
            }
        }

        binding.btnDelete.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            viewModel.deleteDigit()
        }

        binding.btnBiometric.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            launchBiometricPrompt()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updatePinDots(state.pinLength, state.errorMessage != null)
                    updateRateLimitUi(state)
                    binding.btnBiometric.isVisible = state.isBiometricEnabled && biometricAuthManager.canAuthenticate(this@AppLockActivity)

                    if (state.isSuccess) {
                        setResult(RESULT_OK)
                        finish()
                    }

                    if (state.shouldTriggerBiometric && biometricAuthManager.canAuthenticate(this@AppLockActivity)) {
                        viewModel.onBiometricDismissed()
                        launchBiometricPrompt()
                    }
                }
            }
        }
    }

    private fun updatePinDots(length: Int, hasError: Boolean) {
        dots.forEachIndexed { index, dotView ->
            when {
                hasError -> {
                    dotView.setBackgroundResource(R.drawable.bg_pin_dot_error)
                }
                index < length -> {
                    dotView.setBackgroundResource(R.drawable.bg_pin_dot_filled)
                }
                else -> {
                    dotView.setBackgroundResource(R.drawable.bg_pin_dot_empty)
                }
            }
        }

        if (hasError) {
            val shake = AnimationUtils.loadAnimation(this, R.anim.shake_error)
            binding.pinDotsContainer.startAnimation(shake)
        }
    }

    private fun updateRateLimitUi(state: AppLockUiState) {
        val isLockedOut = state.rateLimitState.isLockedOut
        setKeypadEnabled(!isLockedOut)

        if (isLockedOut) {
            binding.tvErrorMessage.isVisible = true
            binding.tvErrorMessage.text = getString(
                R.string.security_lockout_message,
                state.rateLimitState.remainingSeconds
            )
        } else if (state.errorMessage != null) {
            binding.tvErrorMessage.isVisible = true
            binding.tvErrorMessage.text = state.errorMessage
        } else {
            binding.tvErrorMessage.isVisible = false
        }
    }

    private fun setKeypadEnabled(enabled: Boolean) {
        val keypadViews = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9,
            binding.btnDelete, binding.btnBiometric
        )
        keypadViews.forEach { view ->
            view.isEnabled = enabled
            view.alpha = if (enabled) 1.0f else 0.4f
        }
    }

    private fun launchBiometricPrompt() {
        biometricAuthManager.promptBiometric(
            activity = this,
            title = getString(R.string.security_biometric_prompt_title),
            subtitle = getString(R.string.security_biometric_prompt_subtitle),
            negativeButtonText = getString(R.string.security_biometric_prompt_negative),
            onSuccess = {
                viewModel.onBiometricSuccess()
            },
            onError = { _, _ ->
                viewModel.onBiometricDismissed()
            },
            onFailed = {
                viewModel.onBiometricDismissed()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            appLockManager.setLockScreenShowing(false)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AppLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        }
    }
}

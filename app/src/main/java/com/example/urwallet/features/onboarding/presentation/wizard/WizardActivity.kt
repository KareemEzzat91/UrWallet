package com.example.urwallet.features.onboarding.presentation.wizard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.urwallet.MainActivity
import com.example.urwallet.R
import com.example.urwallet.databinding.ActivityWizardBinding
import com.example.urwallet.features.onboarding.presentation.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWizardBinding
    val viewModel: OnboardingViewModel by viewModels()

    private var currentStep = 1
    private val totalSteps = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        goToStep(1)
        setupClickListeners()
        observeNavigation()
    }

    private fun setupInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val topInset = if (statusBars.top > 0) statusBars.top else resources.getDimensionPixelSize(R.dimen.spacing_lg)

            binding.wizardTopBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topInset
            }
            binding.btnWizardNext.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val bottomInset = if (navBars.bottom > 0) navBars.bottom else 0
                bottomMargin = bottomInset + resources.getDimensionPixelSize(R.dimen.spacing_xl)
            }
            insets
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            if (currentStep > 1) {
                goToStep(currentStep - 1)
            } else {
                finish()
            }
        }

        binding.btnSkipWizard.setOnClickListener {
            viewModel.skipWizard()
        }

        binding.btnWizardNext.setOnClickListener {
            if (currentStep < totalSteps) {
                goToStep(currentStep + 1)
            } else {
                // Step 5 complete: save goal & complete
                viewModel.saveGoalAndComplete()
            }
        }
    }

    private fun observeNavigation() {
        lifecycleScope.launch {
            viewModel.navigateToMain.collect { navigate ->
                if (navigate) {
                    viewModel.onNavigatedToMain()
                    startActivity(
                        Intent(this@WizardActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }
            }
        }
    }

    fun goToStep(step: Int) {
        currentStep = step
        val fragment: Fragment = when (step) {
            1 -> WizardStep1Fragment()
            2 -> WizardStep2Fragment()
            3 -> WizardStep3Fragment()
            4 -> WizardStep4Fragment()
            5 -> WizardStep5Fragment()
            else -> return
        }

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.wizard_fragment_container, fragment)
            .commit()

        // Update header
        binding.tvStepIndicator.text = getString(R.string.wizard_step_of, step, totalSteps)
        binding.progressBar.progress = step

        // Last step: change button text & hide skip
        if (step == totalSteps) {
            binding.btnWizardNext.text = getString(R.string.action_start_journey)
            binding.btnSkipWizard.visibility = View.GONE
        } else {
            binding.btnWizardNext.text = getString(R.string.action_next)
            binding.btnSkipWizard.visibility = View.VISIBLE
        }
    }
}

package com.example.urwallet.features.security.presentation.settings

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.urwallet.R
import com.example.urwallet.databinding.BottomSheetPinSetupBinding
import com.example.urwallet.features.security.presentation.biometrics.BiometricAuthManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PinSetupBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPinSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SecuritySettingsViewModel by activityViewModels()

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private val dots by lazy {
        listOf(binding.wDot1, binding.wDot2, binding.wDot3, binding.wDot4)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPinSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mode = arguments?.getString(ARG_MODE) ?: MODE_SETUP
        if (savedInstanceState == null) {
            if (mode == MODE_CHANGE) {
                viewModel.startPinChange()
            } else {
                viewModel.startPinSetup()
            }
        }

        setupKeypad()
        setupBiometricButtons()
        observeState()
    }

    private fun setupKeypad() {
        val numberButtons = listOf(
            binding.wBtn0 to '0',
            binding.wBtn1 to '1',
            binding.wBtn2 to '2',
            binding.wBtn3 to '3',
            binding.wBtn4 to '4',
            binding.wBtn5 to '5',
            binding.wBtn6 to '6',
            binding.wBtn7 to '7',
            binding.wBtn8 to '8',
            binding.wBtn9 to '9'
        )

        numberButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.appendWizardDigit(digit)
            }
        }

        binding.wBtnDelete.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            viewModel.deleteWizardDigit()
        }
    }

    private fun setupBiometricButtons() {
        binding.btnEnableBiometric.setOnClickListener {
            viewModel.completeWizard(enableBiometric = true)
            dismiss()
        }

        binding.btnSkipBiometric.setOnClickListener {
            viewModel.completeWizard(enableBiometric = false)
            dismiss()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.wizardUiState.collect { state ->
                    updateStepUi(state)
                    updateDots(state.pinLength, state.errorMessage != null)

                    if (state.step == PinWizardStep.SUCCESS) {
                        dismiss()
                    }
                }
            }
        }
    }

    private fun updateStepUi(state: PinWizardUiState) {
        val isPinStep = state.step != PinWizardStep.BIOMETRICS && state.step != PinWizardStep.SUCCESS
        binding.containerPinEntry.isVisible = isPinStep
        binding.containerBiometricStep.isVisible = state.step == PinWizardStep.BIOMETRICS

        when (state.step) {
            PinWizardStep.VERIFY_CURRENT -> {
                binding.tvWizardTitle.text = getString(R.string.security_confirm_current_pin)
                binding.tvWizardSubtitle.text = "يرجى إدخال رمز PIN الحالي للمتابعة"
            }
            PinWizardStep.ENTER_NEW -> {
                binding.tvWizardTitle.text = getString(R.string.security_pin_setup_step1_title)
                binding.tvWizardSubtitle.text = getString(R.string.security_pin_setup_step1_subtitle)
            }
            PinWizardStep.CONFIRM_NEW -> {
                binding.tvWizardTitle.text = getString(R.string.security_pin_setup_step2_title)
                binding.tvWizardSubtitle.text = getString(R.string.security_pin_setup_step2_subtitle)
            }
            PinWizardStep.BIOMETRICS -> {
                binding.tvWizardTitle.text = getString(R.string.security_pin_setup_step3_title)
                binding.tvWizardSubtitle.text = getString(R.string.security_pin_setup_step3_subtitle)
                val canBio = biometricAuthManager.canAuthenticate(requireContext())
                binding.btnEnableBiometric.isEnabled = canBio
                if (!canBio) {
                    binding.btnEnableBiometric.text = "البصمة غير متوفرة على الجهاز"
                }
            }
            PinWizardStep.SUCCESS -> { /* dismissed */ }
        }

        if (state.errorMessage != null) {
            binding.tvWizardError.isVisible = true
            binding.tvWizardError.text = state.errorMessage
        } else {
            binding.tvWizardError.isVisible = false
        }
    }

    private fun updateDots(length: Int, hasError: Boolean) {
        dots.forEachIndexed { index, dotView ->
            when {
                hasError -> dotView.setBackgroundResource(R.drawable.bg_pin_dot_error)
                index < length -> dotView.setBackgroundResource(R.drawable.bg_pin_dot_filled)
                else -> dotView.setBackgroundResource(R.drawable.bg_pin_dot_empty)
            }
        }

        if (hasError) {
            val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake_error)
            binding.wizardDotsContainer.startAnimation(shake)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PinSetupBottomSheet"
        const val ARG_MODE = "arg_mode"
        const val MODE_SETUP = "setup"
        const val MODE_CHANGE = "change"

        fun newInstance(mode: String = MODE_SETUP): PinSetupBottomSheetFragment {
            return PinSetupBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode)
                }
            }
        }
    }
}

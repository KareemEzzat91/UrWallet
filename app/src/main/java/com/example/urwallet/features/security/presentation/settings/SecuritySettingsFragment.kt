package com.example.urwallet.features.security.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentSecuritySettingsBinding
import com.example.urwallet.features.security.presentation.biometrics.BiometricAuthManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SecuritySettingsFragment : Fragment() {

    private var _binding: FragmentSecuritySettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SecuritySettingsViewModel by activityViewModels()

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private var isUpdatingUi = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecuritySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupAppLockToggle()
        setupChangePin()
        setupBiometricToggle()
        observeSettings()
        observeEvents()
    }

    private fun setupAppLockToggle() {
        binding.switchAppLock.setOnClickListener {
            val currentState = viewModel.settingsState.value.isAppLockEnabled
            if (!currentState) {
                // User wants to enable: launch setup wizard
                binding.switchAppLock.isChecked = false
                PinSetupBottomSheetFragment.newInstance(PinSetupBottomSheetFragment.MODE_SETUP)
                    .show(childFragmentManager, PinSetupBottomSheetFragment.TAG)
            } else {
                // User wants to disable: require current PIN
                binding.switchAppLock.isChecked = true
                showDisableAppLockDialog()
            }
        }
    }

    private fun showDisableAppLockDialog() {
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = getString(R.string.security_confirm_current_pin)
            maxLines = 1
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.security_disable_lock_dialog_title)
            .setMessage(R.string.security_disable_lock_dialog_desc)
            .setView(input)
            .setPositiveButton(R.string.security_action_confirm) { dialog, _ ->
                val entered = input.text.toString()
                viewModel.disableAppLock(entered) { success, errorMsg ->
                    if (!success) {
                        showSnackbar(errorMsg ?: getString(R.string.security_pin_wrong))
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.security_action_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupChangePin() {
        binding.cardChangePin.setOnClickListener {
            if (viewModel.settingsState.value.isAppLockEnabled) {
                PinSetupBottomSheetFragment.newInstance(PinSetupBottomSheetFragment.MODE_CHANGE)
                    .show(childFragmentManager, PinSetupBottomSheetFragment.TAG)
            }
        }
    }

    private fun setupBiometricToggle() {
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                viewModel.toggleBiometric(isChecked)
            }
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settingsState.collect { state ->
                    isUpdatingUi = true
                    binding.switchAppLock.isChecked = state.isAppLockEnabled
                    binding.cardChangePin.isEnabled = state.isAppLockEnabled
                    binding.cardChangePin.alpha = if (state.isAppLockEnabled) 1.0f else 0.4f

                    val canAuthenticate = biometricAuthManager.canAuthenticate(requireContext())
                    if (!canAuthenticate) {
                        binding.switchBiometric.isEnabled = false
                        binding.switchBiometric.isChecked = false
                        binding.tvBiometricWarning.isVisible = true
                    } else {
                        binding.tvBiometricWarning.isVisible = false
                        binding.switchBiometric.isEnabled = state.isAppLockEnabled
                        binding.switchBiometric.isChecked = state.isBiometricEnabled
                    }
                    isUpdatingUi = false
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is SecurityEvent.PinSetupSuccess -> {
                            showSnackbar(getString(R.string.security_pin_saved_success))
                        }
                        is SecurityEvent.PinChangeSuccess -> {
                            showSnackbar(getString(R.string.security_pin_changed_success))
                        }
                        is SecurityEvent.AppLockDisabled -> {
                            showSnackbar(getString(R.string.security_lock_disabled_success))
                        }
                        is SecurityEvent.ShowSnackbar -> {
                            showSnackbar(event.message)
                        }
                    }
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

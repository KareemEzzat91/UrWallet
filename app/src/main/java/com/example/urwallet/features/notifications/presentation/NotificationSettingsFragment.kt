package com.example.urwallet.features.notifications.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentNotificationSettingsBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationSettingsFragment : Fragment() {

    private var _binding: FragmentNotificationSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationSettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus(isGranted)
        if (isGranted) {
            Toast.makeText(requireContext(), R.string.settings_permission_granted, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), R.string.settings_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeUiState()
        checkNotificationPermissionOnResume()
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermissionOnResume()
    }

    private fun checkNotificationPermissionOnResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            viewModel.updatePermissionStatus(isGranted)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchDailyReminder.isPressed) {
                viewModel.toggleDailyReminder(isChecked)
            }
        }

        binding.btnPickReminderTime.setOnClickListener {
            showMaterialTimePicker()
        }

        binding.rowReminderTime.setOnClickListener {
            showMaterialTimePicker()
        }

        binding.switchBudgetAlerts.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchBudgetAlerts.isPressed) {
                viewModel.toggleBudgetAlerts(isChecked)
            }
        }

        binding.switchGoalAlerts.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchGoalAlerts.isPressed) {
                viewModel.toggleGoalAlerts(isChecked)
            }
        }

        binding.btnSendTestNotification.setOnClickListener {
            viewModel.sendTestNotification()
        }

        binding.btnGrantPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showMaterialTimePicker() {
        val currentSettings = viewModel.uiState.value.settings
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentSettings.reminderHour)
            .setMinute(currentSettings.reminderMinute)
            .setTitleText(R.string.settings_reminder_time_label)
            .build()

        picker.addOnPositiveButtonClickListener {
            viewModel.setReminderTime(picker.hour, picker.minute)
        }

        picker.show(childFragmentManager, "NotificationTimePicker")
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update Switches without recursive listener loop
                    if (binding.switchDailyReminder.isChecked != state.settings.isDailyReminderEnabled) {
                        binding.switchDailyReminder.isChecked = state.settings.isDailyReminderEnabled
                    }
                    if (binding.switchBudgetAlerts.isChecked != state.settings.isBudgetAlertsEnabled) {
                        binding.switchBudgetAlerts.isChecked = state.settings.isBudgetAlertsEnabled
                    }
                    if (binding.switchGoalAlerts.isChecked != state.settings.isGoalAlertsEnabled) {
                        binding.switchGoalAlerts.isChecked = state.settings.isGoalAlertsEnabled
                    }

                    // Daily reminder time display and enabled state
                    binding.btnPickReminderTime.text = state.formattedReminderTime
                    val isReminderEnabled = state.settings.isDailyReminderEnabled
                    binding.rowReminderTime.isEnabled = isReminderEnabled
                    binding.btnPickReminderTime.isEnabled = isReminderEnabled
                    binding.rowReminderTime.alpha = if (isReminderEnabled) 1.0f else 0.4f

                    // Permission banner (Android 13+)
                    binding.cardPermissionBanner.isVisible = state.showPermissionBanner

                    // Test notification toast
                    state.testNotificationMessage?.let { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearTestMessage()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

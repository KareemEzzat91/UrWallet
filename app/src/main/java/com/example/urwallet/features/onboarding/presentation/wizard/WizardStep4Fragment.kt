package com.example.urwallet.features.onboarding.presentation.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urwallet.databinding.FragmentWizardStep4Binding
import com.example.urwallet.features.onboarding.presentation.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WizardStep4Fragment : Fragment() {

    private var _binding: FragmentWizardStep4Binding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWizardStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val state = viewModel.wizardState.value
        binding.switchWeekly.isChecked = state.reminderWeekly
        binding.switchMonthly.isChecked = state.reminderMonthly
        binding.switchDeadline.isChecked = state.reminderDeadline
        binding.switchMotivation.isChecked = state.reminderMotivation

        binding.switchWeekly.setOnCheckedChangeListener { _, checked ->
            viewModel.setReminderWeekly(checked)
        }
        binding.switchMonthly.setOnCheckedChangeListener { _, checked ->
            viewModel.setReminderMonthly(checked)
        }
        binding.switchDeadline.setOnCheckedChangeListener { _, checked ->
            viewModel.setReminderDeadline(checked)
        }
        binding.switchMotivation.setOnCheckedChangeListener { _, checked ->
            viewModel.setReminderMotivation(checked)
        }

        binding.timePicker.setIs24HourView(false)
        binding.timePicker.hour = state.reminderHour
        binding.timePicker.minute = 0
        binding.timePicker.setOnTimeChangedListener { _, hour, _ ->
            viewModel.setReminderHour(hour)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

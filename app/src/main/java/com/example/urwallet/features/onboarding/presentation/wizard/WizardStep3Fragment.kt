package com.example.urwallet.features.onboarding.presentation.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.urwallet.R
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.databinding.FragmentWizardStep3Binding
import com.example.urwallet.features.onboarding.presentation.OnboardingViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WizardStep3Fragment : Fragment() {

    private var _binding: FragmentWizardStep3Binding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWizardStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.paceRelaxed.setOnClickListener { viewModel.setPaceMode(GoalPaceMode.RELAXED) }
        binding.paceBalanced.setOnClickListener { viewModel.setPaceMode(GoalPaceMode.BALANCED) }
        binding.paceAggressive.setOnClickListener { viewModel.setPaceMode(GoalPaceMode.AGGRESSIVE) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wizardState.collect { state ->
                updatePaceUI(state.paceMode)
                updateMonthlyResult(state.monthlyTarget, state.monthsToGoal)
            }
        }
    }

    private fun updatePaceUI(pace: GoalPaceMode) {
        val selectedBg = R.drawable.bg_pace_selected
        val unselectedBg = R.drawable.bg_pace_unselected

        binding.paceRelaxed.setBackgroundResource(if (pace == GoalPaceMode.RELAXED) selectedBg else unselectedBg)
        binding.paceBalanced.setBackgroundResource(if (pace == GoalPaceMode.BALANCED) selectedBg else unselectedBg)
        binding.paceAggressive.setBackgroundResource(if (pace == GoalPaceMode.AGGRESSIVE) selectedBg else unselectedBg)

        // Text colors
        val selectedColor = requireContext().getColor(android.R.color.white)
        val unselectedColor = requireContext().getColor(R.color.urwallet_text_primary)
        val selectedSecondary = 0xCCE8EAF6.toInt()
        val unselectedSecondary = requireContext().getColor(R.color.urwallet_text_secondary)

        binding.tvPaceRelaxed.setTextColor(if (pace == GoalPaceMode.RELAXED) selectedColor else unselectedColor)
        binding.tvPaceBalanced.setTextColor(if (pace == GoalPaceMode.BALANCED) selectedColor else unselectedColor)
        binding.tvPaceAggressive.setTextColor(if (pace == GoalPaceMode.AGGRESSIVE) selectedColor else unselectedColor)
    }

    private fun updateMonthlyResult(monthly: Double, months: Int) {
        val fmt = NumberFormat.getNumberInstance(Locale("ar", "EG")).apply {
            maximumFractionDigits = 0
        }
        binding.tvMonthlyResult.text = getString(R.string.wizard_monthly_calc, fmt.format(monthly), months)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

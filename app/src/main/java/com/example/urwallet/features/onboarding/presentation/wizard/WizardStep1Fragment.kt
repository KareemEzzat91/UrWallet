package com.example.urwallet.features.onboarding.presentation.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentWizardStep1Binding
import com.example.urwallet.features.onboarding.presentation.GoalType
import com.example.urwallet.features.onboarding.presentation.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WizardStep1Fragment : Fragment() {

    private var _binding: FragmentWizardStep1Binding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWizardStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardEmergency.setOnClickListener { selectGoalType(GoalType.EMERGENCY) }
        binding.cardTravel.setOnClickListener { selectGoalType(GoalType.TRAVEL) }
        binding.cardHome.setOnClickListener { selectGoalType(GoalType.HOME) }
        binding.cardRetirement.setOnClickListener { selectGoalType(GoalType.RETIREMENT) }
        binding.cardCustom.setOnClickListener { selectGoalType(GoalType.CUSTOM) }

        // Restore selected state from ViewModel
        selectGoalType(viewModel.wizardState.value.selectedGoalType)
    }

    private fun selectGoalType(type: GoalType) {
        viewModel.setGoalType(type)
        val selected = R.drawable.bg_goal_card_selected
        val unselected = R.drawable.bg_goal_card_unselected
        binding.cardEmergency.setBackgroundResource(if (type == GoalType.EMERGENCY) selected else unselected)
        binding.cardTravel.setBackgroundResource(if (type == GoalType.TRAVEL) selected else unselected)
        binding.cardHome.setBackgroundResource(if (type == GoalType.HOME) selected else unselected)
        binding.cardRetirement.setBackgroundResource(if (type == GoalType.RETIREMENT) selected else unselected)
        binding.cardCustom.setBackgroundResource(if (type == GoalType.CUSTOM) selected else unselected)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

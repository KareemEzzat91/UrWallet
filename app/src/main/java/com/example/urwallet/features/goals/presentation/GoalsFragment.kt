package com.example.urwallet.features.goals.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentGoalsBinding
import com.example.urwallet.features.goals.presentation.adapter.ActiveGoalsAdapter
import com.example.urwallet.features.goals.presentation.adapter.CompletedGoalsAdapter
import com.example.urwallet.features.goals.presentation.add.AddGoalBottomSheetFragment
import com.example.urwallet.features.goals.presentation.contribute.ContributeGoalBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by activityViewModels()

    private lateinit var activeGoalsAdapter: ActiveGoalsAdapter
    private lateinit var completedGoalsAdapter: CompletedGoalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupAddGoalActions()
        observeGoalsState()
    }

    private fun setupAdapters() {
        activeGoalsAdapter = ActiveGoalsAdapter(
            onGoalClick = { goal ->
                findNavController().navigate(
                    R.id.action_goalsFragment_to_goalDetailFragment,
                    bundleOf("goalId" to goal.id)
                )
            },
            onContributeClick = { goal ->
                ContributeGoalBottomSheetFragment.newInstance(
                    goalId = goal.id,
                    goalName = goal.name
                ).show(childFragmentManager, ContributeGoalBottomSheetFragment.TAG)
            }
        )
        binding.rvActiveGoals.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvActiveGoals.adapter = activeGoalsAdapter

        completedGoalsAdapter = CompletedGoalsAdapter(
            onGoalClick = { goal ->
                findNavController().navigate(
                    R.id.action_goalsFragment_to_goalDetailFragment,
                    bundleOf("goalId" to goal.id)
                )
            }
        )
        binding.rvCompletedGoals.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvCompletedGoals.adapter = completedGoalsAdapter
    }

    private fun setupAddGoalActions() {
        val openAddGoalSheet = {
            AddGoalBottomSheetFragment.newInstance()
                .show(childFragmentManager, AddGoalBottomSheetFragment.TAG)
        }

        binding.btnAddGoalHeader.setOnClickListener { openAddGoalSheet() }
        binding.btnEmptyAddGoal.setOnClickListener { openAddGoalSheet() }
    }

    private fun observeGoalsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.goalsUiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: GoalsUiState) {
        when (state) {
            is GoalsUiState.Loading -> {
                binding.progressLoading.isVisible = true
                binding.layoutGoalsContent.isVisible = false
                binding.layoutEmptyState.isVisible = false
            }
            is GoalsUiState.Empty -> {
                binding.progressLoading.isVisible = false
                binding.layoutGoalsContent.isVisible = false
                binding.layoutEmptyState.isVisible = true
            }
            is GoalsUiState.Error -> {
                binding.progressLoading.isVisible = false
                binding.layoutGoalsContent.isVisible = false
                binding.layoutEmptyState.isVisible = false
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
            is GoalsUiState.Success -> {
                binding.progressLoading.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.layoutGoalsContent.isVisible = true

                // Overview Hero Calculations
                val totalSaved = state.activeGoals.sumOf { it.savedAmount } + state.completedGoals.sumOf { it.targetAmount }
                val totalTarget = state.activeGoals.sumOf { it.targetAmount } + state.completedGoals.sumOf { it.targetAmount }
                val totalPercentage = if (totalTarget > 0) ((totalSaved / totalTarget) * 100).toInt().coerceIn(0, 100) else 0

                binding.tvTotalGoalsSavings.text = com.example.urwallet.core.common.Formatters.formatCurrency(totalSaved)
                binding.overviewProgressBar.progress = totalPercentage

                if (state.activeGoals.isNotEmpty()) {
                    binding.tvOverviewActiveCount.text = getString(R.string.goals_overview_active_format, state.activeGoals.size)
                    binding.tvOverviewProgressDesc.text = getString(
                        R.string.goals_overview_progress_format,
                        totalPercentage,
                        com.example.urwallet.core.common.Formatters.formatCurrency(totalTarget)
                    )
                } else {
                    binding.tvOverviewActiveCount.text = getString(R.string.status_completed)
                    binding.tvOverviewProgressDesc.text = getString(R.string.goals_all_completed_format)
                }

                // Active Goals
                val hasActive = state.activeGoals.isNotEmpty()
                binding.layoutActiveSection.isVisible = hasActive
                binding.tvActiveGoalsCount.text = state.activeGoals.size.toString()
                if (hasActive) {
                    activeGoalsAdapter.submitList(state.activeGoals)
                }

                // Completed Goals
                val hasCompleted = state.completedGoals.isNotEmpty()
                binding.layoutCompletedSection.isVisible = hasCompleted
                binding.tvCompletedGoalsCount.text = state.completedGoals.size.toString()
                if (hasCompleted) {
                    completedGoalsAdapter.submitList(state.completedGoals)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

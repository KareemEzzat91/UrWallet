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
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.FragmentGoalsBinding
import com.example.urwallet.features.goals.presentation.adapter.ActiveGoalsAdapter
import com.example.urwallet.features.goals.presentation.adapter.CompletedGoalsAdapter
import com.example.urwallet.features.goals.presentation.add.AddGoalBottomSheetFragment
import com.example.urwallet.features.goals.presentation.contribute.ContributeGoalBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoalsFragment : Fragment() {

    private enum class FilterTab { ACTIVE, COMPLETED, ALL }

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by activityViewModels()

    private lateinit var activeGoalsAdapter: ActiveGoalsAdapter
    private lateinit var completedGoalsAdapter: CompletedGoalsAdapter

    private var currentFilter = FilterTab.ACTIVE
    private var lastSuccessState: GoalsUiState.Success? = null

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
        setupFilterTabs()
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

    private fun setupFilterTabs() {
        binding.tabActiveGoals.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            selectFilter(FilterTab.ACTIVE)
        }
        binding.tabCompletedGoals.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            selectFilter(FilterTab.COMPLETED)
        }
        binding.tabAllGoals.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            selectFilter(FilterTab.ALL)
        }
        updateTabsVisual()
    }

    private fun selectFilter(filter: FilterTab) {
        currentFilter = filter
        updateTabsVisual()
        applyFilterToSections()
    }

    private fun updateTabsVisual() {
        val context = context ?: return
        val primaryColor = context.getColor(R.color.urwallet_text_primary)
        val tertiaryColor = context.getColor(R.color.urwallet_text_tertiary)
        val accentColor = context.getColor(R.color.urwallet_accent)

        // Tab 1: Active
        binding.tvTabActiveLabel.setTextColor(if (currentFilter == FilterTab.ACTIVE) primaryColor else tertiaryColor)
        binding.tvTabActiveLabel.setTypeface(null, if (currentFilter == FilterTab.ACTIVE) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        binding.tvTabActiveCount.setBackgroundResource(if (currentFilter == FilterTab.ACTIVE) R.drawable.bg_squircle_pastel_blue else R.drawable.bg_pill_pace)
        binding.tvTabActiveCount.setTextColor(if (currentFilter == FilterTab.ACTIVE) accentColor else tertiaryColor)

        // Tab 2: Completed
        binding.tvTabCompletedLabel.setTextColor(if (currentFilter == FilterTab.COMPLETED) primaryColor else tertiaryColor)
        binding.tvTabCompletedLabel.setTypeface(null, if (currentFilter == FilterTab.COMPLETED) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        binding.tvTabCompletedCount.setBackgroundResource(if (currentFilter == FilterTab.COMPLETED) R.drawable.bg_pill_income else R.drawable.bg_pill_pace)
        binding.tvTabCompletedCount.setTextColor(if (currentFilter == FilterTab.COMPLETED) context.getColor(R.color.urwallet_income) else tertiaryColor)

        // Tab 3: All
        binding.tvTabAllLabel.setTextColor(if (currentFilter == FilterTab.ALL) primaryColor else tertiaryColor)
        binding.tvTabAllLabel.setTypeface(null, if (currentFilter == FilterTab.ALL) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        binding.tvTabAllCount.setBackgroundResource(if (currentFilter == FilterTab.ALL) R.drawable.bg_squircle_pastel_blue else R.drawable.bg_pill_pace)
        binding.tvTabAllCount.setTextColor(if (currentFilter == FilterTab.ALL) accentColor else tertiaryColor)
    }

    private fun applyFilterToSections() {
        val state = lastSuccessState ?: return
        when (currentFilter) {
            FilterTab.ACTIVE -> {
                binding.layoutActiveSection.isVisible = state.activeGoals.isNotEmpty()
                binding.layoutCompletedSection.isVisible = false
            }
            FilterTab.COMPLETED -> {
                binding.layoutActiveSection.isVisible = false
                binding.layoutCompletedSection.isVisible = state.completedGoals.isNotEmpty()
            }
            FilterTab.ALL -> {
                binding.layoutActiveSection.isVisible = state.activeGoals.isNotEmpty()
                binding.layoutCompletedSection.isVisible = state.completedGoals.isNotEmpty()
            }
        }
    }

    private fun setupAddGoalActions() {
        val openAddGoalSheet = { initialName: String?, initialIcon: String?, initialTarget: Double? ->
            AddGoalBottomSheetFragment.newInstance(initialName, initialIcon, initialTarget)
                .show(childFragmentManager, AddGoalBottomSheetFragment.TAG)
        }

        binding.btnAddGoalHeader.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            openAddGoalSheet(null, null, null)
        }
        binding.btnEmptyAddGoal.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            openAddGoalSheet(null, null, null)
        }

        // Suggested Preset Goal Cards
        binding.cardPresetCar.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            openAddGoalSheet("شراء سيارة", "ic_transport", 150000.0)
        }
        binding.cardPresetHome.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            openAddGoalSheet("شراء منزل", "ic_goal_home", 500000.0)
        }
        binding.cardPresetTravel.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            openAddGoalSheet("رحلة سفر", "ic_goal_travel", 20000.0)
        }
        binding.btnSuggestedViewAll.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            openAddGoalSheet(null, null, null)
        }
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
                lastSuccessState = state
                binding.progressLoading.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.layoutGoalsContent.isVisible = true

                // Overview Hero Calculations
                val totalSaved = state.activeGoals.sumOf { it.savedAmount } + state.completedGoals.sumOf { it.targetAmount }
                val totalTarget = state.activeGoals.sumOf { it.targetAmount } + state.completedGoals.sumOf { it.targetAmount }
                val totalPercentage = if (totalTarget > 0) ((totalSaved / totalTarget) * 100).toInt().coerceIn(0, 100) else 0

                binding.tvTotalGoalsSavings.text = Formatters.formatCurrency(totalSaved, includeDecimals = false)
                binding.overviewProgressBar.progress = totalPercentage
                binding.tvOverviewProgressDesc.text = "من ${Formatters.formatCurrency(totalTarget, includeDecimals = false)}"
                binding.tvOverviewProgressPercentage.text = "$totalPercentage%"

                // Pills Count
                binding.tvOverviewActiveCount.text = state.activeGoals.size.toString()
                binding.tvOverviewCompletedCount.text = state.completedGoals.size.toString()

                // Tabs Count
                binding.tvTabActiveCount.text = state.activeGoals.size.toString()
                binding.tvTabCompletedCount.text = state.completedGoals.size.toString()
                binding.tvTabAllCount.text = (state.activeGoals.size + state.completedGoals.size).toString()

                // Savings Insight Card ("💡 اقتراح ادخار")
                val activeWithMonthly = state.activeGoals.firstOrNull { it.monthlyTarget > 0 }
                if (activeWithMonthly != null) {
                    val monthlyClean = Formatters.formatCurrency(activeWithMonthly.monthlyTarget, includeDecimals = false)
                    binding.tvInsightMessage.text = "خصص $monthlyClean شهرياً للوصول لهدفك في الموعد المحدد."
                    binding.cardSavingsInsight.isVisible = true
                    binding.cardSavingsInsight.setOnClickListener {
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        ContributeGoalBottomSheetFragment.newInstance(
                            goalId = activeWithMonthly.id,
                            goalName = activeWithMonthly.name
                        ).show(childFragmentManager, ContributeGoalBottomSheetFragment.TAG)
                    }
                } else {
                    binding.cardSavingsInsight.isVisible = false
                }

                // Submit adapter lists
                activeGoalsAdapter.submitList(state.activeGoals)
                completedGoalsAdapter.submitList(state.completedGoals)

                applyFilterToSections()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

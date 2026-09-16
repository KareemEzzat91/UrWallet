package com.example.urwallet.features.dashboard.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.databinding.FragmentDashboardBinding
import com.example.urwallet.features.challenges.domain.model.ChallengeProgress
import com.example.urwallet.features.dashboard.domain.model.DashboardSummary
import com.example.urwallet.features.dashboard.presentation.adapter.RecentTransactionsAdapter
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.core.designsystem.performHapticClick
import com.example.urwallet.core.designsystem.startSkeletonShimmer
import com.example.urwallet.core.designsystem.stopSkeletonShimmer
import com.example.urwallet.features.transactions.presentation.AddTransactionBottomSheetFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var recentTransactionsAdapter: RecentTransactionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreeting()
        setupRecentTransactionsList()
        setupNavigationLinks()
        observeDashboardState()
    }

    private fun setupGreeting() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (currentHour) {
            in 4..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
        binding.tvGreeting.text = getString(greetingRes)
    }

    private fun setupRecentTransactionsList() {
        recentTransactionsAdapter = RecentTransactionsAdapter {
            // Tapping a recent transaction switches to Transactions tab
            navigateToTab(R.id.transactionsFragment)
        }
        binding.rvRecentTransactions.adapter = recentTransactionsAdapter

        // Tap on empty state to launch Add Transaction Bottom Sheet
        binding.layoutEmptyRecent.setOnClickListener {
            it.performHapticClick()
            AddTransactionBottomSheetFragment.newInstance()
                .show(parentFragmentManager, AddTransactionBottomSheetFragment.TAG)
        }
    }

    private fun setupNavigationLinks() {
        binding.btnNotifications.setOnClickListener {
            it.performHapticClick()
            findNavController().navigate(R.id.action_dashboardFragment_to_notificationSettingsFragment)
        }

        binding.btnViewAllTransactions.setOnClickListener {
            it.performHapticClick()
            navigateToTab(R.id.transactionsFragment)
        }

        binding.cardNearestGoal.setOnClickListener {
            it.performHapticClick()
            navigateToTab(R.id.goalsFragment)
        }
    }

    private fun navigateToTab(tabMenuId: Int) {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)?.selectedItemId = tabMenuId
    }

    private fun observeDashboardState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboardUiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: DashboardUiState) {
        when (state) {
            is DashboardUiState.Loading -> {
                binding.layoutSkeleton.root.isVisible = true
                binding.layoutSkeleton.root.startSkeletonShimmer()
                binding.layoutDashboardContent.isVisible = false
                binding.tvErrorMessage.isVisible = false
            }
            is DashboardUiState.Error -> {
                binding.layoutSkeleton.root.stopSkeletonShimmer()
                binding.layoutSkeleton.root.isVisible = false
                binding.layoutDashboardContent.isVisible = false
                binding.tvErrorMessage.isVisible = true
                binding.tvErrorMessage.text = state.message
            }
            is DashboardUiState.Success -> {
                binding.layoutSkeleton.root.stopSkeletonShimmer()
                binding.layoutSkeleton.root.isVisible = false
                binding.layoutDashboardContent.isVisible = true
                binding.tvErrorMessage.isVisible = false
                bindSummaryData(state.summary)
            }
        }
    }

    private fun bindSummaryData(summary: DashboardSummary) {
        // 1. Hero Balance & Month
        binding.tvNetBalance.text = Formatters.formatCurrency(summary.netBalance)
        binding.tvCurrentMonth.text = DateUtils.formatMonthYearArabic(
            DateUtils.getCurrentMonth(),
            DateUtils.getCurrentYear()
        )

        // 2. Monthly Stats
        binding.tvMonthlyIncome.text = Formatters.formatSignedAmount(
            amount = summary.monthlyIncome,
            type = TransactionType.INCOME
        )
        binding.tvMonthlyExpense.text = Formatters.formatSignedAmount(
            amount = summary.monthlyExpense,
            type = TransactionType.EXPENSE
        )

        // 3. Nearest Active Goal Card
        bindNearestGoal(summary.nearestGoal)

        // 4. Active Challenge Card
        bindActiveChallenge(summary.activeChallenge)

        // 5. Recent Transactions
        if (summary.recentTransactions.isNotEmpty()) {
            binding.rvRecentTransactions.isVisible = true
            binding.layoutEmptyRecent.isVisible = false
            recentTransactionsAdapter.submitList(summary.recentTransactions)
        } else {
            binding.rvRecentTransactions.isVisible = false
            binding.layoutEmptyRecent.isVisible = true
        }
    }

    private fun bindNearestGoal(goal: Goal?) {
        if (goal == null) {
            binding.cardNearestGoal.isVisible = false
            return
        }

        binding.cardNearestGoal.isVisible = true
        binding.tvGoalName.text = goal.name
        binding.tvGoalPercentage.text = Formatters.formatPercentage(goal.progressPercentage)
        binding.progressGoal.progress = goal.progressPercentage.toInt().coerceIn(0, 100)

        binding.tvGoalSaved.text = getString(
            R.string.dashboard_goal_saved_format,
            Formatters.formatCurrency(goal.savedAmount)
        )
        binding.tvGoalTarget.text = getString(
            R.string.dashboard_goal_target_format,
            Formatters.formatCurrency(goal.targetAmount)
        )

        // Goal Icon: if drawable name starts with "ic_", use ImageView, otherwise display Emoji
        if (goal.icon.startsWith("ic_")) {
            binding.ivGoalIcon.isVisible = true
            binding.tvGoalEmoji.isVisible = false
            val iconRes = CategoryIconMapper.getIconDrawableRes(goal.icon)
            binding.ivGoalIcon.setImageResource(iconRes)
        } else {
            binding.ivGoalIcon.isVisible = false
            binding.tvGoalEmoji.isVisible = true
            binding.tvGoalEmoji.text = goal.icon.ifBlank { "🎯" }
        }
    }

    private fun bindActiveChallenge(challengeProgress: ChallengeProgress?) {
        if (challengeProgress == null) {
            binding.cardActiveChallenge.isVisible = false
            return
        }

        binding.cardActiveChallenge.isVisible = true
        val challenge = challengeProgress.challenge
        binding.tvDashboardChallengeTitle.text = challenge.title
        binding.tvDashboardStreak.text = getString(R.string.streak_days_format, challengeProgress.currentStreak)

        val pct = challengeProgress.progressPercentage.toInt()
        binding.progressDashboardChallenge.progress = pct

        val desc = when (challenge.type) {
            ChallengeType.NO_SPENDING -> {
                val target = challengeProgress.targetDays ?: 7
                getString(R.string.challenge_progress_days_format, challengeProgress.completedDays, target)
            }
            ChallengeType.SAVE_AMOUNT -> {
                val target = challengeProgress.targetAmount ?: 0.0
                val currentFormatted = Formatters.formatCurrency(challengeProgress.currentProgress)
                val targetFormatted = Formatters.formatCurrency(target)
                getString(R.string.challenge_progress_amount_format, currentFormatted, targetFormatted)
            }
            ChallengeType.REDUCE_CATEGORY -> {
                val limit = challengeProgress.targetAmount ?: 0.0
                val spentFormatted = Formatters.formatCurrency(challengeProgress.currentProgress)
                val limitFormatted = Formatters.formatCurrency(limit)
                getString(R.string.challenge_progress_limit_format, spentFormatted, limitFormatted)
            }
        }
        binding.tvDashboardChallengeDesc.text = desc
        binding.tvDashboardChallengeRemaining.text = "باقي ${challengeProgress.remainingDays} أيام"

        binding.cardActiveChallenge.setOnClickListener {
            val bundle = androidx.core.os.bundleOf("challengeId" to challenge.id)
            findNavController().navigate(R.id.action_dashboardFragment_to_challengeDetailFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.urwallet.features.dashboard.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.core.designsystem.performHapticClick
import com.example.urwallet.core.designsystem.startSkeletonShimmer
import com.example.urwallet.core.designsystem.stopSkeletonShimmer
import com.example.urwallet.databinding.FragmentDashboardBinding
import com.example.urwallet.features.challenges.domain.model.ChallengeProgress
import com.example.urwallet.features.dashboard.domain.model.DashboardSummary
import com.example.urwallet.features.dashboard.presentation.adapter.RecentTransactionsAdapter
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.presentation.contribute.ContributeGoalBottomSheetFragment
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

    private var isBalanceHidden = false
    private var currentNetBalance: Double = 0.0
    private var currentAvailableCash: Double = 0.0
    private var currentGoalSavings: Double = 0.0
    private var nearestGoal: Goal? = null

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

        setupInsets()
        setupGreeting()
        setupRecentTransactionsList()
        setupNavigationLinks()
        setupQuickActions()
        setupPrivacyToggle()
        observeDashboardState()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutTopHeader) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = statusBars.top)
            insets
        }
    }

    private fun setupGreeting() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (currentHour) {
            in 4..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
        binding.tvGreeting.text = getString(greetingRes)

        val dayOfWeekFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
        val dayOfWeek = dayOfWeekFormat.format(calendar.time)
        val monthYear = DateUtils.formatMonthYearLocalized(
            DateUtils.getCurrentMonth(),
            DateUtils.getCurrentYear()
        )
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val separator = if (java.util.Locale.getDefault().language == "ar") "، " else ", "
        binding.tvSubtitle.text = "$dayOfWeek$separator$day $monthYear"
    }

    private fun setupRecentTransactionsList() {
        recentTransactionsAdapter = RecentTransactionsAdapter {
            navigateToTab(R.id.transactionsFragment)
        }
        binding.rvRecentTransactions.adapter = recentTransactionsAdapter

        binding.layoutEmptyRecent.setOnClickListener {
            it.performHapticClick()
            showAddTransactionSheet()
        }

        binding.btnEmptyAddTransaction.setOnClickListener {
            it.performHapticClick()
            showAddTransactionSheet()
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

    private fun setupQuickActions() {
        binding.btnQuickExpense.setOnClickListener {
            it.performHapticClick()
            showAddTransactionSheet()
        }

        binding.btnQuickIncome.setOnClickListener {
            it.performHapticClick()
            showAddTransactionSheet()
        }

        binding.btnQuickGoal.setOnClickListener {
            it.performHapticClick()
            val goal = nearestGoal
            if (goal != null) {
                ContributeGoalBottomSheetFragment.newInstance(goal.id, goal.name)
                    .show(parentFragmentManager, ContributeGoalBottomSheetFragment.TAG)
            } else {
                navigateToTab(R.id.goalsFragment)
            }
        }

        binding.btnQuickAnalytics.setOnClickListener {
            it.performHapticClick()
            navigateToTab(R.id.moreFragment)
        }
    }

    private fun setupPrivacyToggle() {
        binding.btnToggleBalancePrivacy.setOnClickListener {
            it.performHapticClick()
            isBalanceHidden = !isBalanceHidden
            renderBalance()
        }
    }

    private fun renderBalance() {
        if (isBalanceHidden) {
            binding.tvNetBalance.text = getString(R.string.dashboard_balance_hidden_mask)
            binding.tvAvailableCash.text = getString(R.string.dashboard_balance_hidden_mask)
            binding.tvAvailableCash.isVisible = currentGoalSavings > 0.001
            binding.btnToggleBalancePrivacy.setImageResource(R.drawable.ic_eye_closed)
        } else {
            val formatted = if (currentNetBalance < 0) {
                "\u200E-${Formatters.formatCurrency(kotlin.math.abs(currentNetBalance))}"
            } else {
                Formatters.formatCurrency(currentNetBalance)
            }
            binding.tvNetBalance.text = formatted
            if (currentGoalSavings > 0.001) {
                val availableFormatted = Formatters.formatCurrency(currentAvailableCash)
                val savingsFormatted = Formatters.formatCurrency(currentGoalSavings)
                binding.tvAvailableCash.text = "السيولة المتاحة: $availableFormatted (مدخر بالأهداف: $savingsFormatted)"
                binding.tvAvailableCash.isVisible = true
            } else {
                binding.tvAvailableCash.isVisible = false
            }
            binding.btnToggleBalancePrivacy.setImageResource(R.drawable.ic_eye_open)
        }
    }

    private fun showAddTransactionSheet() {
        AddTransactionBottomSheetFragment.newInstance()
            .show(parentFragmentManager, AddTransactionBottomSheetFragment.TAG)
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
        currentNetBalance = summary.netBalance
        currentAvailableCash = summary.availableCash
        currentGoalSavings = summary.totalGoalSavings
        renderBalance()

        binding.tvCurrentMonth.text = getString(R.string.dashboard_this_month)

        // 2. Monthly Stats
        binding.tvMonthlyIncome.text = Formatters.formatCurrency(summary.monthlyIncome)
        binding.tvMonthlyExpense.text = Formatters.formatCurrency(summary.monthlyExpense)

        // 3. Nearest Active Goal Card
        nearestGoal = summary.nearestGoal
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

        // Accurate 0% progress handling (hide indicator color when 0 to avoid rounded cap dot)
        val progressVal = goal.progressPercentage.toInt().coerceIn(0, 100)
        if (progressVal == 0) {
            binding.progressGoal.setIndicatorColor(android.graphics.Color.TRANSPARENT)
            binding.progressGoal.progress = 0
        } else {
            binding.progressGoal.setIndicatorColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.urwallet_accent)
            )
            binding.progressGoal.progress = progressVal
        }

        // Format: "0 / 5,000 ج.م"
        val savedNum = String.format(java.util.Locale.US, "%,.0f", goal.savedAmount)
        val targetFormatted = Formatters.formatCurrency(goal.targetAmount)
        binding.tvGoalSaved.text = "$savedNum / $targetFormatted"

        // Format: "متبقي 5,000 ج.م"
        val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
        binding.tvGoalTarget.text = getString(
            R.string.dashboard_goal_remaining_format,
            Formatters.formatCurrency(remaining)
        )

        if (goal.icon.startsWith("ic_")) {
            binding.ivGoalIcon.isVisible = true
            binding.tvGoalEmoji.isVisible = false
            val iconRes = CategoryIconMapper.getIconDrawableRes(goal.icon)
            binding.ivGoalIcon.setImageResource(iconRes)
        } else {
            binding.ivGoalIcon.isVisible = false
            binding.tvGoalEmoji.isVisible = true
            binding.tvGoalEmoji.text = goal.icon.ifBlank { "🛡️" }
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

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
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.databinding.FragmentDashboardBinding
import com.example.urwallet.features.dashboard.domain.model.DashboardSummary
import com.example.urwallet.features.dashboard.presentation.adapter.RecentTransactionsAdapter
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.core.designsystem.CategoryIconMapper
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
            // Tapping a recent transaction navigates to Transactions tab
            findNavController().navigate(R.id.transactionsFragment)
        }
        binding.rvRecentTransactions.adapter = recentTransactionsAdapter
    }

    private fun setupNavigationLinks() {
        binding.btnViewAllTransactions.setOnClickListener {
            findNavController().navigate(R.id.transactionsFragment)
        }

        binding.cardNearestGoal.setOnClickListener {
            findNavController().navigate(R.id.goalsFragment)
        }
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
                binding.progressLoading.isVisible = true
                binding.layoutDashboardContent.isVisible = false
                binding.tvErrorMessage.isVisible = false
            }
            is DashboardUiState.Error -> {
                binding.progressLoading.isVisible = false
                binding.layoutDashboardContent.isVisible = false
                binding.tvErrorMessage.isVisible = true
                binding.tvErrorMessage.text = state.message
            }
            is DashboardUiState.Success -> {
                binding.progressLoading.isVisible = false
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

        // 4. Recent Transactions
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

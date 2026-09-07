package com.example.urwallet.features.goals.presentation.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.databinding.FragmentGoalDetailBinding
import com.example.urwallet.features.goals.domain.model.GoalDetail
import com.example.urwallet.features.goals.presentation.adapter.GoalContributionsAdapter
import com.example.urwallet.features.goals.presentation.contribute.ContributeGoalBottomSheetFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class GoalDetailFragment : Fragment() {

    private var _binding: FragmentGoalDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalDetailViewModel by viewModels()
    private lateinit var contributionsAdapter: GoalContributionsAdapter

    private var currentDetail: GoalDetail? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Read safe args if passed manually or via NavArgs
        arguments?.getLong("goalId", -1L)?.let { id ->
            if (id != -1L) viewModel.setGoalId(id)
        }

        setupToolbar()
        setupContributionsList()
        setupContributeAction()
        observeDetailState()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnDeleteGoal.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupContributionsList() {
        contributionsAdapter = GoalContributionsAdapter()
        binding.rvContributions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvContributions.adapter = contributionsAdapter
    }

    private fun setupContributeAction() {
        binding.btnContributeAction.setOnClickListener {
            val detail = currentDetail ?: return@setOnClickListener
            ContributeGoalBottomSheetFragment.newInstance(
                goalId = detail.goal.id,
                goalName = detail.goal.name
            ).show(childFragmentManager, ContributeGoalBottomSheetFragment.TAG)
        }
    }

    private fun observeDetailState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detailUiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: GoalDetailUiState) {
        when (state) {
            is GoalDetailUiState.Loading -> {
                binding.progressLoading.isVisible = true
                binding.layoutGoalDetailContent.isVisible = false
            }
            is GoalDetailUiState.Error -> {
                binding.progressLoading.isVisible = false
                binding.layoutGoalDetailContent.isVisible = false
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            is GoalDetailUiState.Success -> {
                binding.progressLoading.isVisible = false
                binding.layoutGoalDetailContent.isVisible = true
                bindGoalDetail(state.detail)
            }
        }
    }

    private fun bindGoalDetail(detail: GoalDetail) {
        currentDetail = detail

        // Header & Icon
        binding.ivGoalDetailIcon.setImageResource(CategoryIconMapper.getIconDrawableRes(detail.goal.icon))
        binding.tvGoalDetailName.text = detail.goal.name

        // Radial Progress
        val progressInt = detail.progressPercentage.toInt().coerceIn(0, 100)
        binding.detailProgressIndicator.progress = progressInt
        binding.tvDetailProgressPercentage.text = String.format(Locale.getDefault(), "%d%%", progressInt)

        if (detail.isCompleted) {
            binding.tvDetailStatusBadge.text = getString(R.string.status_completed)
            binding.tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_pill_income)
            binding.tvDetailStatusBadge.setTextColor(resources.getColor(R.color.urwallet_income, null))
            binding.btnContributeAction.isEnabled = false
            binding.btnContributeAction.text = getString(R.string.goal_completed_celebration)
        } else {
            binding.tvDetailStatusBadge.text = getString(R.string.status_active)
            binding.tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_pill_primary)
            binding.tvDetailStatusBadge.setTextColor(resources.getColor(R.color.urwallet_primary, null))
            binding.btnContributeAction.isEnabled = true
            binding.btnContributeAction.text = getString(R.string.action_contribute_full)
        }

        // Financials
        binding.tvDetailSavedAmount.text = Formatters.formatCurrency(detail.savedAmount)
        binding.tvDetailRemainingAmount.text = Formatters.formatCurrency(detail.remainingAmount)
        binding.tvDetailTargetAmount.text = Formatters.formatCurrency(detail.targetAmount)

        // Metrics Grid
        binding.tvRequiredMonthlySavings.text = Formatters.formatCurrency(detail.requiredMonthlySavings)
        binding.tvDetailDaysRemaining.text = getString(R.string.goals_days_left_format, detail.daysRemaining)

        // Contributions List
        if (detail.contributions.isNotEmpty()) {
            binding.rvContributions.isVisible = true
            binding.tvEmptyContributions.isVisible = false
            contributionsAdapter.submitList(detail.contributions)
        } else {
            binding.rvContributions.isVisible = false
            binding.tvEmptyContributions.isVisible = true
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_goal_title)
            .setMessage(R.string.dialog_delete_goal_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteGoal { success ->
                    if (success) {
                        Toast.makeText(requireContext(), R.string.goal_deleted_success, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

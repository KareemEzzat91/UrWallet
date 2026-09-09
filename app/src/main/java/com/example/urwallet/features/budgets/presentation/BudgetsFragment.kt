package com.example.urwallet.features.budgets.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.core.common.BudgetStatus
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.FragmentBudgetsBinding
import com.example.urwallet.features.budgets.domain.model.BudgetSummary
import com.example.urwallet.features.budgets.domain.model.BudgetsSummaryResult
import com.example.urwallet.features.budgets.presentation.adapter.CategoryBudgetsAdapter
import com.example.urwallet.features.budgets.presentation.add.AddBudgetBottomSheetFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class BudgetsFragment : Fragment() {

    private var _binding: FragmentBudgetsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetsViewModel by activityViewModels()
    private lateinit var categoryBudgetsAdapter: CategoryBudgetsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupAdapter()
        setupAddBudgetActions()
        observeBudgetsState()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupAdapter() {
        categoryBudgetsAdapter = CategoryBudgetsAdapter(
            onDeleteClick = { budget ->
                showDeleteConfirmationDialog(budget)
            }
        )
        binding.rvCategoryBudgets.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvCategoryBudgets.adapter = categoryBudgetsAdapter
    }

    private fun setupAddBudgetActions() {
        val openAddBudgetSheet = {
            AddBudgetBottomSheetFragment.newInstance()
                .show(childFragmentManager, AddBudgetBottomSheetFragment.TAG)
        }

        binding.btnAddBudgetHeader.setOnClickListener { openAddBudgetSheet() }
        binding.btnEmptyAddBudget.setOnClickListener { openAddBudgetSheet() }
    }

    private fun observeBudgetsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.budgetsUiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: BudgetsUiState) {
        when (state) {
            is BudgetsUiState.Loading -> {
                binding.progressLoading.isVisible = true
                binding.layoutBudgetsContent.isVisible = false
                binding.layoutEmptyState.isVisible = false
            }
            is BudgetsUiState.Empty -> {
                binding.progressLoading.isVisible = false
                binding.layoutBudgetsContent.isVisible = false
                binding.layoutEmptyState.isVisible = true
            }
            is BudgetsUiState.Error -> {
                binding.progressLoading.isVisible = false
                binding.layoutBudgetsContent.isVisible = false
                binding.layoutEmptyState.isVisible = false
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
            is BudgetsUiState.Success -> {
                binding.progressLoading.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.layoutBudgetsContent.isVisible = true

                renderBudgetsSummary(state.result)
            }
        }
    }

    private fun renderBudgetsSummary(result: BudgetsSummaryResult) {
        val context = requireContext()

        // 1. Global Budget Hero Card
        val gb = result.globalBudget
        if (gb != null) {
            binding.cardGlobalBudget.isVisible = true
            binding.tvGlobalSpent.text = Formatters.formatCurrency(gb.spentAmount)
            binding.tvGlobalLimitDesc.text = "من إجمالي ميزانية ${Formatters.formatCurrency(gb.limitAmount)}"
            binding.progressGlobalBudget.progress = gb.visualProgress
            binding.tvGlobalPercentage.text = String.format(Locale.getDefault(), "%.0f%%", gb.progressPercentage)

            when (gb.status) {
                BudgetStatus.HEALTHY -> {
                    binding.tvGlobalStatus.text = getString(R.string.budget_status_healthy)
                    binding.progressGlobalBudget.setIndicatorColor(context.getColor(R.color.urwallet_income_light))
                    binding.tvGlobalRemaining.text = "${getString(R.string.budget_remaining_label)} ${Formatters.formatCurrency(gb.remainingAmount)}"
                }
                BudgetStatus.NEAR_LIMIT -> {
                    binding.tvGlobalStatus.text = getString(R.string.budget_status_near_limit)
                    binding.progressGlobalBudget.setIndicatorColor(context.getColor(R.color.urwallet_warning))
                    binding.tvGlobalRemaining.text = "${getString(R.string.budget_remaining_label)} ${Formatters.formatCurrency(gb.remainingAmount)}"
                }
                BudgetStatus.EXCEEDED -> {
                    binding.tvGlobalStatus.text = getString(R.string.budget_status_exceeded)
                    binding.progressGlobalBudget.setIndicatorColor(context.getColor(R.color.urwallet_expense_light))
                    binding.tvGlobalRemaining.text = "${getString(R.string.budget_overspent_label)} ${Formatters.formatCurrency(gb.overspentAmount)}"
                }
            }
        } else {
            binding.cardGlobalBudget.isVisible = false
        }

        // 2. Category Budgets Section
        val hasCategories = result.categoryBudgets.isNotEmpty()
        binding.layoutCategoriesSection.isVisible = hasCategories
        if (hasCategories) {
            binding.tvCategoriesCount.text = result.categoryBudgets.size.toString()
            categoryBudgetsAdapter.submitList(result.categoryBudgets)
        }
    }

    private fun showDeleteConfirmationDialog(budget: BudgetSummary) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_budget_title)
            .setMessage(R.string.dialog_delete_budget_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteBudget(budget.budgetId) { result ->
                    if (result.isSuccess) {
                        Toast.makeText(requireContext(), R.string.budget_deleted_success, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            result.exceptionOrNull()?.localizedMessage ?: "فشل حذف الميزانية",
                            Toast.LENGTH_SHORT
                        ).show()
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

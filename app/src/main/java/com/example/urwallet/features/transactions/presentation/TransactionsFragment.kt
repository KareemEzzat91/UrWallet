package com.example.urwallet.features.transactions.presentation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.core.designsystem.performHapticClick
import com.example.urwallet.core.designsystem.showSuccessSnackbar
import com.example.urwallet.core.designsystem.startSkeletonShimmer
import com.example.urwallet.core.designsystem.stopSkeletonShimmer
import com.example.urwallet.databinding.FragmentTransactionsBinding
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.presentation.adapter.GroupedTransactionAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by activityViewModels()
    private lateinit var transactionAdapter: GroupedTransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackPressHandler()
        setupRecyclerView()
        setupSelectionMode()
        setupSearch()
        setupFilters()
        setupReportsBanner()
        setupEmptyState()
        observeTransactions()
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.isSelectionMode.value) {
                        viewModel.exitSelectionMode()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun setupEmptyState() {
        binding.btnEmptyAddTransaction.setOnClickListener {
            it.performHapticClick()
            AddTransactionBottomSheetFragment.newInstance()
                .show(parentFragmentManager, AddTransactionBottomSheetFragment.TAG)
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = GroupedTransactionAdapter(
            onTransactionClick = { transaction ->
                findNavController().navigate(
                    R.id.action_transactionsFragment_to_transactionDetailFragment,
                    bundleOf("transactionId" to transaction.id)
                )
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction.id)
            },
            onToggleSelection = { transaction ->
                viewModel.toggleTransactionSelection(transaction.id)
            },
            onTransactionLongClick = { transaction ->
                binding.root.performHapticClick()
                viewModel.enterSelectionMode(transaction.id)
                true
            }
        )
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun setupSelectionMode() {
        binding.btnCloseSelection.setOnClickListener {
            it.performHapticClick()
            viewModel.exitSelectionMode()
        }

        binding.btnBulkDelete.setOnClickListener {
            it.performHapticClick()
            val count = viewModel.selectedTransactionIds.value.size
            if (count > 0) {
                showBulkDeleteConfirmationDialog(count)
            }
        }

        binding.btnBulkChangeCategory.setOnClickListener {
            it.performHapticClick()
            val categories = viewModel.allCategories.value
            if (categories.isNotEmpty()) {
                showBulkChangeCategoryDialog(categories)
            } else {
                Toast.makeText(requireContext(), "لا توجد فئات متاحة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val query = s?.toString().orEmpty()
                binding.ivClearSearch.isVisible = query.isNotEmpty()
                viewModel.setSearchQuery(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
        }

        binding.btnResetFilters.setOnClickListener {
            binding.etSearch.setText("")
            viewModel.clearAllFilters()
        }
    }

    private fun setupFilters() {
        binding.chipGroupQuickType.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipExpenses) -> viewModel.setQuickTypeFilter(TransactionType.EXPENSE)
                checkedIds.contains(R.id.chipIncome) -> viewModel.setQuickTypeFilter(TransactionType.INCOME)
                else -> viewModel.setQuickTypeFilter(null)
            }
        }

        binding.btnAdvancedFilter.setOnClickListener {
            it.performHapticClick()
            FilterBottomSheetFragment.newInstance()
                .show(childFragmentManager, FilterBottomSheetFragment.TAG)
        }

        binding.btnHeaderFilter.setOnClickListener {
            it.performHapticClick()
            FilterBottomSheetFragment.newInstance()
                .show(childFragmentManager, FilterBottomSheetFragment.TAG)
        }

        binding.btnDateFilter.setOnClickListener {
            it.performHapticClick()
            FilterBottomSheetFragment.newInstance()
                .show(childFragmentManager, FilterBottomSheetFragment.TAG)
        }
    }

    private fun setupReportsBanner() {
        binding.btnViewReports.setOnClickListener {
            it.performHapticClick()
            findNavController().navigate(
                R.id.action_transactionsFragment_to_categoryAnalyticsFragment,
                bundleOf("categoryId" to -1L)
            )
        }

        binding.btnCloseReportsBanner.setOnClickListener {
            it.performHapticClick()
            binding.cardReportsBanner.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.cardReportsBanner.visibility = View.GONE
                }
        }
    }

    private fun showDeleteConfirmationDialog(transactionId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_transaction_title)
            .setMessage(R.string.dialog_delete_transaction_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteTransaction(transactionId)
                showSuccessSnackbar(getString(R.string.transaction_deleted_success))
            }
            .show()
    }

    private fun showBulkDeleteConfirmationDialog(selectedCount: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("حذف المعاملات المحددة")
            .setMessage("هل أنت متأكد من حذف $selectedCount معاملات؟ لا يمكن التراجع عن هذا الإجراء.")
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.bulkDeleteSelected(
                    onSuccess = { count ->
                        showSuccessSnackbar("تم حذف $count معاملات بنجاح")
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .show()
    }

    private fun showBulkChangeCategoryDialog(categories: List<com.example.urwallet.features.transactions.domain.model.Category>) {
        if (categories.isEmpty()) return
        val categoryNames = categories.map { it.name }.toTypedArray()
        var selectedIndex = 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("اختر الفئة الجديدة")
            .setSingleChoiceItems(categoryNames, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton("تطبيق") { _, _ ->
                val chosenCategory = categories.getOrNull(selectedIndex) ?: return@setPositiveButton
                viewModel.bulkChangeCategorySelected(
                    newCategoryId = chosenCategory.id,
                    onSuccess = { count ->
                        showSuccessSnackbar("تم تحديث فئة $count معاملات بنجاح إلى ${chosenCategory.name}")
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .show()
    }

    private fun observeTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.transactionsUiState.collect { state ->
                        renderState(state)
                    }
                }
                launch {
                    viewModel.filterCriteria.collect { criteria ->
                        syncQuickTypeChips(criteria.type)
                        binding.viewFilterBadge.isVisible = criteria.hasActiveFilters()
                    }
                }
            }
        }
    }

    private fun syncQuickTypeChips(type: TransactionType?) {
        when (type) {
            TransactionType.EXPENSE -> {
                if (!binding.chipExpenses.isChecked) binding.chipExpenses.isChecked = true
            }
            TransactionType.INCOME -> {
                if (!binding.chipIncome.isChecked) binding.chipIncome.isChecked = true
            }
            null -> {
                if (!binding.chipAll.isChecked) binding.chipAll.isChecked = true
            }
        }
    }

    private fun renderState(state: TransactionsUiState) {
        when (state) {
            is TransactionsUiState.Loading -> {
                binding.layoutNormalHeader.isVisible = true
                binding.layoutSelectionHeader.isVisible = false
                binding.layoutSkeletonTransactions.isVisible = true
                binding.layoutSkeletonTransactions.startSkeletonShimmer()
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.layoutNoSearchResults.isVisible = false
                binding.tvErrorMessage.isVisible = false
            }
            is TransactionsUiState.Empty -> {
                binding.layoutNormalHeader.isVisible = true
                binding.layoutSelectionHeader.isVisible = false
                binding.layoutSkeletonTransactions.stopSkeletonShimmer()
                binding.layoutSkeletonTransactions.isVisible = false
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = true
                binding.layoutNoSearchResults.isVisible = false
                binding.tvErrorMessage.isVisible = false
            }
            is TransactionsUiState.NoSearchResults -> {
                binding.layoutNormalHeader.isVisible = true
                binding.layoutSelectionHeader.isVisible = false
                binding.layoutSkeletonTransactions.stopSkeletonShimmer()
                binding.layoutSkeletonTransactions.isVisible = false
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.layoutNoSearchResults.isVisible = true
                binding.tvErrorMessage.isVisible = false
            }
            is TransactionsUiState.Success -> {
                binding.layoutNormalHeader.isVisible = !state.isSelectionMode
                binding.layoutSelectionHeader.isVisible = state.isSelectionMode
                if (state.isSelectionMode) {
                    binding.tvSelectionCount.text = "${state.selectedCount} محدد"
                    binding.btnBulkDelete.isEnabled = state.selectedCount > 0
                    binding.btnBulkChangeCategory.isEnabled = state.selectedCount > 0
                }
                binding.layoutSkeletonTransactions.stopSkeletonShimmer()
                binding.layoutSkeletonTransactions.isVisible = false
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = true
                binding.layoutEmptyState.isVisible = false
                binding.layoutNoSearchResults.isVisible = false
                binding.tvErrorMessage.isVisible = false
                transactionAdapter.submitList(state.items)
            }
            is TransactionsUiState.Error -> {
                binding.layoutNormalHeader.isVisible = true
                binding.layoutSelectionHeader.isVisible = false
                binding.layoutSkeletonTransactions.stopSkeletonShimmer()
                binding.layoutSkeletonTransactions.isVisible = false
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.layoutNoSearchResults.isVisible = false
                binding.tvErrorMessage.isVisible = true
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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

        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupReportsBanner()
        observeTransactions()
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
            }
        )
        binding.rvTransactions.adapter = transactionAdapter
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
            FilterBottomSheetFragment.newInstance()
                .show(childFragmentManager, FilterBottomSheetFragment.TAG)
        }
    }

    private fun setupReportsBanner() {
        binding.btnViewReports.setOnClickListener {
            findNavController().navigate(
                R.id.action_transactionsFragment_to_categoryAnalyticsFragment,
                bundleOf("categoryId" to -1L)
            )
        }
    }

    private fun showDeleteConfirmationDialog(transactionId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_transaction_title)
            .setMessage(R.string.dialog_delete_transaction_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteTransaction(transactionId)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.transaction_deleted_success),
                    Toast.LENGTH_SHORT
                ).show()
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
                binding.progressBar.isVisible = true
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.layoutNoSearchResults.isVisible = false
                binding.tvErrorMessage.isVisible = false
            }
            is TransactionsUiState.Empty -> {
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = true
                binding.layoutNoSearchResults.isVisible = false
                binding.tvErrorMessage.isVisible = false
            }
            is TransactionsUiState.NoSearchResults -> {
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.layoutNoSearchResults.isVisible = true
                binding.tvErrorMessage.isVisible = false
            }
            is TransactionsUiState.Success -> {
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = true
                binding.layoutEmptyState.isVisible = false
                binding.layoutNoSearchResults.isVisible = false
                binding.tvErrorMessage.isVisible = false
                transactionAdapter.submitList(state.items)
            }
            is TransactionsUiState.Error -> {
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

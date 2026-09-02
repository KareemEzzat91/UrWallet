package com.example.urwallet.features.transactions.presentation

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
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentTransactionsBinding
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
        observeTransactions()
    }

    private fun setupRecyclerView() {
        transactionAdapter = GroupedTransactionAdapter(
            onTransactionClick = { transaction ->
                // Future: open detail
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction.id)
            }
        )
        binding.rvTransactions.adapter = transactionAdapter
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
                viewModel.transactionsUiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: TransactionsUiState) {
        when (state) {
            is TransactionsUiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.tvErrorMessage.isVisible = false
            }
            is TransactionsUiState.Empty -> {
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = true
                binding.tvErrorMessage.isVisible = false
            }
            is TransactionsUiState.Success -> {
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = true
                binding.layoutEmptyState.isVisible = false
                binding.tvErrorMessage.isVisible = false
                transactionAdapter.submitList(state.items)
            }
            is TransactionsUiState.Error -> {
                binding.progressBar.isVisible = false
                binding.rvTransactions.isVisible = false
                binding.layoutEmptyState.isVisible = false
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

package com.example.urwallet.features.more.presentation.recurring

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentRecurringBinding
import com.example.urwallet.features.more.domain.model.RecurringTransactionWithCategory
import com.example.urwallet.features.more.presentation.recurring.adapter.RecurringAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@AndroidEntryPoint
class RecurringFragment : Fragment() {

    private var _binding: FragmentRecurringBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecurringViewModel by activityViewModels()

    private lateinit var adapter: RecurringAdapter
    private val decimalFormat = DecimalFormat("#,##0.00")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = RecurringAdapter(
            onToggleActive = { item, isChecked ->
                viewModel.toggleActive(item.recurring.id, isChecked)
            },
            onDelete = { item ->
                showDeleteDialog(item)
            }
        )

        binding.rvRecurring.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecurring.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabAddRecurring.setOnClickListener {
            openAddRecurringBottomSheet()
        }

        binding.btnEmptyAdd.setOnClickListener {
            openAddRecurringBottomSheet()
        }

        binding.chipFilterAll.setOnClickListener {
            viewModel.setFilter(RecurringFilterType.ALL)
        }

        binding.chipFilterExpense.setOnClickListener {
            viewModel.setFilter(RecurringFilterType.EXPENSE)
        }

        binding.chipFilterIncome.setOnClickListener {
            viewModel.setFilter(RecurringFilterType.INCOME)
        }
    }

    private fun openAddRecurringBottomSheet() {
        AddRecurringBottomSheetFragment.show(childFragmentManager)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderUi(state)
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is RecurringUiEvent.ShowMessage -> {
                                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                            }
                            is RecurringUiEvent.DismissAddSheet -> {
                                // Handled inside bottom sheet
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderUi(state: RecurringUiState) {
        val currencySymbol = getString(R.string.currency_symbol)

        binding.tvMonthlyObligations.text = "${decimalFormat.format(state.monthlyObligations)} $currencySymbol"
        binding.tvMonthlyIncome.text = "+ ${decimalFormat.format(state.monthlyRecurringIncome)} $currencySymbol"
        binding.tvActiveCount.text = "${state.activeSubscriptionsCount} ${getString(R.string.recurring_active_count)}"

        adapter.submitList(state.filteredItems)

        if (state.filteredItems.isEmpty()) {
            binding.llEmptyState.visibility = View.VISIBLE
            binding.rvRecurring.visibility = View.GONE
        } else {
            binding.llEmptyState.visibility = View.GONE
            binding.rvRecurring.visibility = View.VISIBLE
        }
    }

    private fun showDeleteDialog(item: RecurringTransactionWithCategory) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_recurring_title)
            .setMessage(R.string.dialog_delete_recurring_msg)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteRecurring(item.recurring.id)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

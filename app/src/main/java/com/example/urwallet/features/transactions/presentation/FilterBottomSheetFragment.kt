package com.example.urwallet.features.transactions.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.urwallet.R
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.databinding.FragmentFilterBottomSheetBinding
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.FilterPeriod
import com.example.urwallet.features.transactions.domain.model.TransactionFilterCriteria
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by activityViewModels()

    private var customStartDate: Long? = null
    private var customEndDate: Long? = null
    private val selectedCategoryIds = mutableSetOf<Long>()
    private var availableCategories = emptyList<Category>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeData()
    }

    private fun setupListeners() {
        binding.ivClose.setOnClickListener { dismiss() }

        binding.chipPeriodCustom.setOnClickListener {
            showDateRangePicker()
        }

        binding.btnReset.setOnClickListener {
            viewModel.clearAllFilters()
            dismiss()
        }

        binding.btnApply.setOnClickListener {
            applyFilters()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCategories.collect { categories ->
                        availableCategories = categories
                        populateCategoryChips(viewModel.filterCriteria.value.categoryIds)
                    }
                }
                launch {
                    viewModel.filterCriteria.collect { criteria ->
                        bindCurrentCriteria(criteria)
                    }
                }
            }
        }
    }

    private fun bindCurrentCriteria(criteria: TransactionFilterCriteria) {
        // 1. Type
        when (criteria.type) {
            TransactionType.EXPENSE -> binding.chipTypeExpense.isChecked = true
            TransactionType.INCOME -> binding.chipTypeIncome.isChecked = true
            null -> binding.chipTypeAll.isChecked = true
        }

        // 2. Categories
        selectedCategoryIds.clear()
        selectedCategoryIds.addAll(criteria.categoryIds)
        populateCategoryChips(selectedCategoryIds)

        // 3. Period
        when (criteria.period) {
            FilterPeriod.ALL -> binding.chipPeriodAll.isChecked = true
            FilterPeriod.TODAY -> binding.chipPeriodToday.isChecked = true
            FilterPeriod.THIS_WEEK -> binding.chipPeriodWeek.isChecked = true
            FilterPeriod.THIS_MONTH -> binding.chipPeriodMonth.isChecked = true
            FilterPeriod.CUSTOM -> {
                binding.chipPeriodCustom.isChecked = true
                customStartDate = criteria.customStartDate
                customEndDate = criteria.customEndDate
                updateCustomDateText()
            }
        }

        // 4. Amounts
        binding.etMinAmount.setText(criteria.minAmount?.toString() ?: "")
        binding.etMaxAmount.setText(criteria.maxAmount?.toString() ?: "")
    }

    private fun populateCategoryChips(selectedIds: Set<Long>) {
        binding.chipGroupCategories.removeAllViews()

        // "الكل" Chip
        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.filter_all)
            isCheckable = true
            isChecked = selectedIds.isEmpty()
            setChipBackgroundColorResource(R.color.urwallet_surface)
            setOnClickListener {
                if (isChecked) {
                    selectedCategoryIds.clear()
                    for (i in 1 until binding.chipGroupCategories.childCount) {
                        (binding.chipGroupCategories.getChildAt(i) as? Chip)?.isChecked = false
                    }
                }
            }
        }
        binding.chipGroupCategories.addView(allChip)

        // Dynamic category chips
        for (category in availableCategories) {
            val chip = Chip(requireContext()).apply {
                text = category.name
                isCheckable = true
                isChecked = selectedIds.contains(category.id)
                setChipBackgroundColorResource(R.color.urwallet_surface)
                setOnCheckedChangeListener { _, isCheckedNow ->
                    if (isCheckedNow) {
                        selectedCategoryIds.add(category.id)
                        allChip.isChecked = false
                    } else {
                        selectedCategoryIds.remove(category.id)
                        if (selectedCategoryIds.isEmpty()) {
                            allChip.isChecked = true
                        }
                    }
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.period_custom))
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            customStartDate = selection.first
            customEndDate = selection.second
            updateCustomDateText()
        }

        picker.addOnNegativeButtonClickListener {
            if (customStartDate == null) {
                binding.chipPeriodAll.isChecked = true
            }
        }

        picker.show(childFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun updateCustomDateText() {
        val start = customStartDate
        val end = customEndDate
        if (start != null && end != null) {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))
            val startStr = formatter.format(Date(start))
            val endStr = formatter.format(Date(end))
            binding.tvCustomDateRange.text = "من $startStr إلى $endStr"
            binding.tvCustomDateRange.isVisible = true
        } else {
            binding.tvCustomDateRange.isVisible = false
        }
    }

    private fun applyFilters() {
        val minStr = binding.etMinAmount.text.toString().trim()
        val maxStr = binding.etMaxAmount.text.toString().trim()

        val minAmount = minStr.toDoubleOrNull()
        val maxAmount = maxStr.toDoubleOrNull()

        // Validation: min > max
        if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
            binding.tvAmountError.text = getString(R.string.error_invalid_amount_range)
            binding.tvAmountError.isVisible = true
            return
        }
        binding.tvAmountError.isVisible = false

        val selectedType = when {
            binding.chipTypeExpense.isChecked -> TransactionType.EXPENSE
            binding.chipTypeIncome.isChecked -> TransactionType.INCOME
            else -> null
        }

        val selectedPeriod = when {
            binding.chipPeriodToday.isChecked -> FilterPeriod.TODAY
            binding.chipPeriodWeek.isChecked -> FilterPeriod.THIS_WEEK
            binding.chipPeriodMonth.isChecked -> FilterPeriod.THIS_MONTH
            binding.chipPeriodCustom.isChecked -> FilterPeriod.CUSTOM
            else -> FilterPeriod.ALL
        }

        val newCriteria = TransactionFilterCriteria(
            type = selectedType,
            categoryIds = selectedCategoryIds.toSet(),
            period = selectedPeriod,
            customStartDate = if (selectedPeriod == FilterPeriod.CUSTOM) customStartDate else null,
            customEndDate = if (selectedPeriod == FilterPeriod.CUSTOM) customEndDate else null,
            minAmount = minAmount,
            maxAmount = maxAmount
        )

        viewModel.applyFilterCriteria(newCriteria)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheetFragment"
        fun newInstance() = FilterBottomSheetFragment()
    }
}

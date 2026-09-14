package com.example.urwallet.features.more.presentation.recurring

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.databinding.BottomSheetAddRecurringBinding
import com.example.urwallet.features.transactions.presentation.adapter.CategoryChipAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddRecurringBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddRecurringBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecurringViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryChipAdapter

    private var selectedType = TransactionType.EXPENSE
    private var selectedCategoryId: Long? = null
    private var selectedFrequency = Frequency.MONTHLY
    private var selectedStartDate = System.currentTimeMillis()
    private var selectedEndDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddRecurringBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryRecyclerView()
        setupTypeToggle()
        setupFrequencyChips()
        setupDatePickers()
        setupSaveButton()
        observeState()
    }

    private fun setupCategoryRecyclerView() {
        categoryAdapter = CategoryChipAdapter { category ->
            selectedCategoryId = category.id
            categoryAdapter.selectedCategoryId = category.id
            binding.tvFormError.isVisible = false
        }
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupTypeToggle() {
        binding.btnExpense.setOnClickListener {
            selectedType = TransactionType.EXPENSE
            viewModel.setSelectedTypeForAdd(TransactionType.EXPENSE)
            updateTypeButtonStyles()
        }

        binding.btnIncome.setOnClickListener {
            selectedType = TransactionType.INCOME
            viewModel.setSelectedTypeForAdd(TransactionType.INCOME)
            updateTypeButtonStyles()
        }

        updateTypeButtonStyles()
    }

    private fun updateTypeButtonStyles() {
        val context = requireContext()
        if (selectedType == TransactionType.EXPENSE) {
            binding.btnExpense.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.urwallet_expense)
            )
            binding.btnExpense.setTextColor(ContextCompat.getColor(context, R.color.white))

            binding.btnIncome.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.urwallet_income_container)
            )
            binding.btnIncome.setTextColor(ContextCompat.getColor(context, R.color.urwallet_income))
        } else {
            binding.btnExpense.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.urwallet_surface)
            )
            binding.btnExpense.setTextColor(ContextCompat.getColor(context, R.color.urwallet_text_secondary))

            binding.btnIncome.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.urwallet_income)
            )
            binding.btnIncome.setTextColor(ContextCompat.getColor(context, R.color.white))
        }
    }

    private fun setupFrequencyChips() {
        binding.chipGroupFrequency.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedFrequency = when {
                checkedIds.contains(R.id.chipDaily) -> Frequency.DAILY
                checkedIds.contains(R.id.chipWeekly) -> Frequency.WEEKLY
                checkedIds.contains(R.id.chipYearly) -> Frequency.YEARLY
                else -> Frequency.MONTHLY
            }
        }
    }

    private fun setupDatePickers() {
        binding.etStartDate.setText(DateUtils.formatDisplayDate(selectedStartDate))

        binding.etStartDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.label_recurring_start_date)
                .setSelection(selectedStartDate)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selectedStartDate = selection
                binding.etStartDate.setText(DateUtils.formatDisplayDate(selection))
            }
            picker.show(childFragmentManager, "START_DATE_PICKER")
        }

        binding.switchHasEndDate.setOnCheckedChangeListener { _, isChecked ->
            binding.tilEndDate.isVisible = isChecked
            if (!isChecked) {
                selectedEndDate = null
                binding.etEndDate.text = null
            } else if (selectedEndDate == null) {
                selectedEndDate = selectedStartDate + (30L * 24 * 60 * 60 * 1000)
                binding.etEndDate.setText(DateUtils.formatDisplayDate(selectedEndDate!!))
            }
        }

        binding.etEndDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.label_recurring_end_date)
                .setSelection(selectedEndDate ?: selectedStartDate)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selectedEndDate = selection
                binding.etEndDate.setText(DateUtils.formatDisplayDate(selection))
            }
            picker.show(childFragmentManager, "END_DATE_PICKER")
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveRecurring.setOnClickListener {
            val title = binding.etTitle.text?.toString().orEmpty().trim()
            val amountStr = binding.etAmount.text?.toString().orEmpty().trim()

            if (title.isBlank()) {
                showError(getString(R.string.error_empty_recurring_title))
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                showError(getString(R.string.error_invalid_recurring_amount))
                return@setOnClickListener
            }

            val categoryId = selectedCategoryId
            if (categoryId == null) {
                showError(getString(R.string.error_recurring_no_category))
                return@setOnClickListener
            }

            if (selectedEndDate != null && selectedEndDate!! < selectedStartDate) {
                showError(getString(R.string.error_invalid_end_date))
                return@setOnClickListener
            }

            binding.tvFormError.isVisible = false
            binding.progressSaving.isVisible = true
            binding.btnSaveRecurring.isEnabled = false

            viewModel.addRecurring(
                title = title,
                amount = amount,
                type = selectedType,
                categoryId = categoryId,
                frequency = selectedFrequency,
                startDate = selectedStartDate,
                endDate = selectedEndDate
            )
        }
    }

    private fun showError(message: String) {
        binding.tvFormError.text = message
        binding.tvFormError.isVisible = true
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categoriesForAdd.collect { categories ->
                        categoryAdapter.submitList(categories)
                        if (selectedCategoryId == null && categories.isNotEmpty()) {
                            selectedCategoryId = categories.first().id
                            categoryAdapter.selectedCategoryId = categories.first().id
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is RecurringUiEvent.DismissAddSheet -> {
                                dismiss()
                            }
                            is RecurringUiEvent.ShowMessage -> {
                                binding.progressSaving.isVisible = false
                                binding.btnSaveRecurring.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddRecurringBottomSheetFragment"

        fun show(fragmentManager: FragmentManager) {
            AddRecurringBottomSheetFragment().show(fragmentManager, TAG)
        }
    }
}

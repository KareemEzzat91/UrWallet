package com.example.urwallet.features.budgets.presentation.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentAddBudgetBottomSheetBinding
import com.example.urwallet.features.budgets.presentation.BudgetsViewModel
import com.example.urwallet.features.transactions.domain.model.Category
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddBudgetBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddBudgetBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetsViewModel by activityViewModels()

    private var isCategoryBudget: Boolean = false
    private var selectedCategoryId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBudgetBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTypeSelector()
        setupPresets()
        setupSaveButton()
        observeCategories()
    }

    private fun setupTypeSelector() {
        binding.chipGroupBudgetType.setOnCheckedStateChangeListener { _, checkedIds ->
            isCategoryBudget = checkedIds.contains(R.id.chipTypeCategory)
            binding.layoutCategorySelection.isVisible = isCategoryBudget
            if (!isCategoryBudget) {
                selectedCategoryId = null
                binding.chipGroupCategories.clearCheck()
            }
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.expenseCategories.collect { categories ->
                    populateCategories(categories)
                }
            }
        }
    }

    private fun populateCategories(categories: List<Category>) {
        binding.chipGroupCategories.removeAllViews()
        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = category.name
                isCheckable = true
                tag = category.id
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategoryId = category.id
                    } else if (selectedCategoryId == category.id) {
                        selectedCategoryId = null
                    }
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun setupPresets() {
        fun addPreset(value: Double) {
            val current = binding.etBudgetAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
            val updated = current + value
            binding.etBudgetAmount.setText(String.format(java.util.Locale.US, "%.0f", updated))
            binding.etBudgetAmount.setSelection(binding.etBudgetAmount.text?.length ?: 0)
        }

        binding.btnPreset500.setOnClickListener { addPreset(500.0) }
        binding.btnPreset1000.setOnClickListener { addPreset(1000.0) }
        binding.btnPreset2000.setOnClickListener { addPreset(2000.0) }
        binding.btnPreset5000.setOnClickListener { addPreset(5000.0) }
    }

    private fun setupSaveButton() {
        binding.btnSaveBudget.setOnClickListener {
            val amountStr = binding.etBudgetAmount.text?.toString().orEmpty().trim()
            val amount = amountStr.toDoubleOrNull() ?: 0.0

            if (amount <= 0.0) {
                binding.etBudgetAmount.error = getString(R.string.error_invalid_budget_amount)
                return@setOnClickListener
            }

            if (isCategoryBudget && selectedCategoryId == null) {
                Toast.makeText(requireContext(), R.string.error_no_category_selected, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSaveBudget.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val result = viewModel.saveBudget(
                    categoryId = if (isCategoryBudget) selectedCategoryId else null,
                    amount = amount
                )

                if (result.isSuccess) {
                    Toast.makeText(requireContext(), R.string.budget_saved_success, Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    binding.btnSaveBudget.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        result.exceptionOrNull()?.localizedMessage ?: "فشل حفظ الميزانية",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddBudgetBottomSheetFragment"

        fun newInstance(): AddBudgetBottomSheetFragment {
            return AddBudgetBottomSheetFragment()
        }
    }
}

package com.example.urwallet.features.transactions.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.urwallet.R
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.databinding.FragmentAddTransactionBottomSheetBinding
import com.example.urwallet.features.transactions.presentation.adapter.CategoryChipAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddTransactionBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddTransactionBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryChipAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryRecyclerView()
        setupTypeToggle()
        setupSaveButton()
        observeState()
    }

    private fun setupCategoryRecyclerView() {
        categoryAdapter = CategoryChipAdapter { category ->
            viewModel.selectCategory(category.id)
        }
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupTypeToggle() {
        binding.btnExpense.setOnClickListener {
            viewModel.selectType(TransactionType.EXPENSE)
        }

        binding.btnIncome.setOnClickListener {
            viewModel.selectType(TransactionType.INCOME)
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {
            val amount = binding.etAmount.text?.toString().orEmpty()
            val title = binding.etTitle.text?.toString().orEmpty()
            val note = binding.etNote.text?.toString()
            viewModel.saveTransaction(amount, title, note)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addTransactionUiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: AddTransactionUiState) {
        val context = requireContext()

        // 1. Update Type Toggle Styling
        if (state.selectedType == TransactionType.EXPENSE) {
            binding.btnExpense.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.urwallet_expense)
            )
            binding.btnExpense.setTextColor(
                ContextCompat.getColor(context, R.color.white)
            )
            binding.btnIncome.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.urwallet_income_container)
            )
            binding.btnIncome.setTextColor(
                ContextCompat.getColor(context, R.color.urwallet_income)
            )
        } else {
            binding.btnExpense.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.urwallet_expense_container)
            )
            binding.btnExpense.setTextColor(
                ContextCompat.getColor(context, R.color.urwallet_expense)
            )
            binding.btnIncome.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.urwallet_income)
            )
            binding.btnIncome.setTextColor(
                ContextCompat.getColor(context, R.color.white)
            )
        }

        // 2. Categories
        val hasCats = !state.noCategoriesAvailable
        binding.rvCategories.isVisible = hasCats
        binding.tvNoCategoriesMessage.isVisible = !hasCats
        if (hasCats) {
            categoryAdapter.submitList(state.categories)
            categoryAdapter.selectedCategoryId = state.selectedCategoryId
        }

        // 3. Error Feedback
        if (state.errorMessage != null) {
            binding.tvFormError.text = state.errorMessage
            binding.tvFormError.isVisible = true
        } else {
            binding.tvFormError.isVisible = false
        }

        // 4. Loading indicator
        binding.progressSaving.isVisible = state.isLoading
        // Disable Save when: loading OR no categories available
        binding.btnSaveTransaction.isEnabled = !state.isLoading && hasCats
        binding.btnSaveTransaction.text = if (state.isLoading) "" else getString(R.string.action_save)

        // 5. Success Handling
        if (state.isSaved) {
            Toast.makeText(
                context,
                getString(R.string.transaction_saved_success),
                Toast.LENGTH_SHORT
            ).show()
            viewModel.resetAddTransactionState()
            dismiss()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTransactionBottomSheet"

        fun newInstance(): AddTransactionBottomSheetFragment {
            return AddTransactionBottomSheetFragment()
        }
    }
}

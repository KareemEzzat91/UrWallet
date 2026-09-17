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
import com.example.urwallet.core.designsystem.UrWalletFeedback
import com.example.urwallet.core.designsystem.performHapticClick
import com.example.urwallet.core.designsystem.performHapticWarning
import com.example.urwallet.databinding.FragmentAddTransactionBottomSheetBinding
import com.example.urwallet.features.transactions.domain.model.Transaction
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
        checkEditMode(savedInstanceState)
        observeState()
    }

    private fun checkEditMode(savedInstanceState: Bundle?) {
        val editId = arguments?.getLong(ARG_EDIT_ID, -1L) ?: -1L
        if (editId > 0 && savedInstanceState == null) {
            val amount = arguments?.getDouble(ARG_EDIT_AMOUNT) ?: 0.0
            val typeStr = arguments?.getString(ARG_EDIT_TYPE)
            val type = typeStr?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() } ?: TransactionType.EXPENSE
            val categoryId = arguments?.getLong(ARG_EDIT_CATEGORY_ID) ?: 1L
            val title = arguments?.getString(ARG_EDIT_TITLE).orEmpty()
            val note = arguments?.getString(ARG_EDIT_NOTE)
            val date = arguments?.getLong(ARG_EDIT_DATE) ?: System.currentTimeMillis()

            binding.etAmount.setText(if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString())
            binding.etTitle.setText(title)
            binding.etNote.setText(note.orEmpty())
            binding.tvSheetTitle.text = getString(R.string.title_edit_transaction)
            binding.btnSaveTransaction.text = getString(R.string.action_edit)

            viewModel.prepareEditTransaction(
                Transaction(
                    id = editId,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    title = title,
                    note = note,
                    date = date
                )
            )
        }
    }

    private fun setupCategoryRecyclerView() {
        categoryAdapter = CategoryChipAdapter { category ->
            binding.root.performHapticClick()
            viewModel.selectCategory(category.id)
        }
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupTypeToggle() {
        binding.btnExpense.setOnClickListener {
            it.performHapticClick()
            viewModel.selectType(TransactionType.EXPENSE)
        }
        binding.btnIncome.setOnClickListener {
            it.performHapticClick()
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
                launch {
                    viewModel.addTransactionUiState.collect { state ->
                        renderState(state)
                    }
                }
                launch {
                    viewModel.transactionUiEvents.collect { event ->
                        activity?.let { act ->
                            val root = act.findViewById<View>(android.R.id.content)
                            if (root != null) {
                                val message = when (event) {
                                    TransactionUiEvent.Updated -> getString(R.string.transaction_updated_success)
                                    TransactionUiEvent.Saved -> getString(R.string.transaction_saved_success)
                                }
                                UrWalletFeedback.showSuccessSnackbar(root, message)
                            }
                        }
                        viewModel.resetAddTransactionState()
                        dismiss()
                    }
                }
            }
        }
    }

    private fun renderState(state: AddTransactionUiState) {
        val context = requireContext()

        // 1. Update Sheet Title & Button Label
        if (state.isEditMode) {
            binding.tvSheetTitle.text = getString(R.string.title_edit_transaction)
            binding.btnSaveTransaction.text = if (state.isLoading) "" else getString(R.string.action_edit)
        } else {
            binding.tvSheetTitle.text = getString(R.string.title_add_transaction)
            binding.btnSaveTransaction.text = if (state.isLoading) "" else getString(R.string.action_save)
        }

        // 2. Update Type Toggle Styling
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

        // 3. Categories
        val hasCats = !state.noCategoriesAvailable
        binding.rvCategories.isVisible = hasCats
        binding.tvNoCategoriesMessage.isVisible = !hasCats
        if (hasCats) {
            categoryAdapter.submitList(state.categories)
            categoryAdapter.selectedCategoryId = state.selectedCategoryId
        }

        // 4. Error Feedback
        if (state.errorMessage != null) {
            binding.tvFormError.text = state.errorMessage
            binding.tvFormError.isVisible = true
            binding.root.performHapticWarning()
        } else {
            binding.tvFormError.isVisible = false
        }

        // 5. Loading indicator
        binding.progressSaving.isVisible = state.isLoading
        // Disable Save when: loading OR no categories available
        binding.btnSaveTransaction.isEnabled = !state.isLoading && hasCats
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTransactionBottomSheet"
        private const val ARG_EDIT_ID = "arg_edit_id"
        private const val ARG_EDIT_AMOUNT = "arg_edit_amount"
        private const val ARG_EDIT_TYPE = "arg_edit_type"
        private const val ARG_EDIT_CATEGORY_ID = "arg_edit_category_id"
        private const val ARG_EDIT_TITLE = "arg_edit_title"
        private const val ARG_EDIT_NOTE = "arg_edit_note"
        private const val ARG_EDIT_DATE = "arg_edit_date"

        fun newInstance(): AddTransactionBottomSheetFragment {
            return AddTransactionBottomSheetFragment()
        }

        fun newInstanceForEdit(transaction: Transaction): AddTransactionBottomSheetFragment {
            return AddTransactionBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_EDIT_ID, transaction.id)
                    putDouble(ARG_EDIT_AMOUNT, transaction.amount)
                    putString(ARG_EDIT_TYPE, transaction.type.name)
                    putLong(ARG_EDIT_CATEGORY_ID, transaction.categoryId)
                    putString(ARG_EDIT_TITLE, transaction.title)
                    putString(ARG_EDIT_NOTE, transaction.note)
                    putLong(ARG_EDIT_DATE, transaction.date)
                }
            }
        }
    }
}

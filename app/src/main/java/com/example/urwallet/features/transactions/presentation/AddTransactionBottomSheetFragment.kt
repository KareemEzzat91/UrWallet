package com.example.urwallet.features.transactions.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentAddTransactionBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddTransactionBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddTransactionBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var isExpenseSelected = true

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
        setupToggleButtons()
        setupSaveButton()
    }

    private fun setupToggleButtons() {
        updateToggleStyles()

        binding.btnExpense.setOnClickListener {
            isExpenseSelected = true
            updateToggleStyles()
        }

        binding.btnIncome.setOnClickListener {
            isExpenseSelected = false
            updateToggleStyles()
        }
    }

    private fun updateToggleStyles() {
        val context = requireContext()
        if (isExpenseSelected) {
            binding.btnExpense.setBackgroundColor(
                ContextCompat.getColor(context, R.color.urwallet_expense)
            )
            binding.btnExpense.setTextColor(
                ContextCompat.getColor(context, R.color.white)
            )
            binding.btnIncome.setBackgroundColor(
                ContextCompat.getColor(context, R.color.urwallet_income_container)
            )
            binding.btnIncome.setTextColor(
                ContextCompat.getColor(context, R.color.urwallet_income)
            )
        } else {
            binding.btnExpense.setBackgroundColor(
                ContextCompat.getColor(context, R.color.urwallet_expense_container)
            )
            binding.btnExpense.setTextColor(
                ContextCompat.getColor(context, R.color.urwallet_expense)
            )
            binding.btnIncome.setBackgroundColor(
                ContextCompat.getColor(context, R.color.urwallet_income)
            )
            binding.btnIncome.setTextColor(
                ContextCompat.getColor(context, R.color.white)
            )
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.feature_coming_soon_toast),
                Toast.LENGTH_SHORT
            ).show()
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

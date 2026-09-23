package com.example.urwallet.features.people.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.urwallet.R
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.BottomSheetSettleObligationBinding
import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettleObligationBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSettleObligationBinding? = null
    private val binding get() = _binding!!

    private var obligationId: Long = 0L
    private var title: String = ""
    private var totalAmount: Double = 0.0
    private var remainingAmount: Double = 0.0

    var onSettleConfirmed: ((obligationId: Long, amount: Double, note: String?, createTransaction: Boolean) -> Unit)? = null

    companion object {
        const val TAG = "SettleObligationBottomSheet"
        private const val ARG_ID = "arg_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_TOTAL = "arg_total"
        private const val ARG_REMAINING = "arg_remaining"

        fun newInstance(obligation: FinancialObligation): SettleObligationBottomSheetFragment {
            val fragment = SettleObligationBottomSheetFragment()
            fragment.arguments = Bundle().apply {
                putLong(ARG_ID, obligation.id)
                putString(ARG_TITLE, obligation.reason ?: "التزام مالي")
                putDouble(ARG_TOTAL, obligation.amount)
                putDouble(ARG_REMAINING, obligation.remainingAmount)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            obligationId = it.getLong(ARG_ID)
            title = it.getString(ARG_TITLE, "")
            totalAmount = it.getDouble(ARG_TOTAL)
            remainingAmount = it.getDouble(ARG_REMAINING)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSettleObligationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSettleObligationTitle.text = title
        binding.tvSettleTotalAmount.text = "الإجمالي: ${Formatters.formatCurrency(totalAmount)}"
        binding.tvSettleRemainingAmount.text = "المتبقي: ${Formatters.formatCurrency(remainingAmount)}"

        // Pre-fill with remaining amount
        val prefillStr = if (remainingAmount % 1.0 == 0.0) {
            remainingAmount.toLong().toString()
        } else {
            remainingAmount.toString()
        }
        binding.etSettlementAmount.setText(prefillStr)

        binding.btnConfirmSettlement.setOnClickListener {
            val amountStr = binding.etSettlementAmount.text?.toString()?.trim().orEmpty()
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                binding.tilSettlementAmount.error = getString(R.string.error_settlement_amount_invalid)
                return@setOnClickListener
            }

            if (amount > remainingAmount + 0.001) {
                binding.tilSettlementAmount.error = getString(
                    R.string.error_settlement_exceeds_remaining,
                    Formatters.formatCurrency(remainingAmount)
                )
                return@setOnClickListener
            }
            binding.tilSettlementAmount.error = null

            val note = binding.etSettlementNotes.text?.toString()?.trim()
            val createTransaction = binding.switchCreateTransaction.isChecked

            onSettleConfirmed?.invoke(obligationId, amount, note, createTransaction)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

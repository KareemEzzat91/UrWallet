package com.example.urwallet.features.people.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.databinding.BottomSheetAddObligationBinding
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker

class AddObligationBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddObligationBinding? = null
    private val binding get() = _binding!!

    private var selectedDueDate: Long? = null
    var onSaveObligation: ((title: String, amount: Double, direction: ObligationDirection, dueDate: Long?, notes: String?) -> Unit)? = null

    companion object {
        const val TAG = "AddObligationBottomSheet"
        fun newInstance(): AddObligationBottomSheetFragment = AddObligationBottomSheetFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddObligationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPickDueDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("اختر تاريخ الاستحقاق")
                .setSelection(selectedDueDate ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selectedDueDate = selection
                binding.tvSelectedDueDate.text = "تاريخ الاستحقاق: ${DateUtils.formatDisplayDate(selection)}"
            }
            picker.show(parentFragmentManager, "OBLIGATION_DUE_DATE_PICKER")
        }

        binding.btnSaveObligation.setOnClickListener {
            val title = binding.etObligationTitle.text?.toString()?.trim().orEmpty()
            if (title.isBlank()) {
                binding.tilObligationTitle.error = getString(R.string.error_obligation_title_empty)
                return@setOnClickListener
            }
            binding.tilObligationTitle.error = null

            val amountStr = binding.etObligationAmount.text?.toString()?.trim().orEmpty()
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                binding.tilObligationAmount.error = getString(R.string.error_obligation_amount_invalid)
                return@setOnClickListener
            }
            binding.tilObligationAmount.error = null

            val direction = if (binding.chipOwedToMe.isChecked) {
                ObligationDirection.OWED_TO_ME
            } else {
                ObligationDirection.I_OWE
            }

            val notes = binding.etObligationNotes.text?.toString()?.trim()

            onSaveObligation?.invoke(title, amount, direction, selectedDueDate, notes)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

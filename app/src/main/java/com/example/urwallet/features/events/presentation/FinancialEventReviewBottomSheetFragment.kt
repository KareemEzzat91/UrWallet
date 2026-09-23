package com.example.urwallet.features.events.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.databinding.BottomSheetFinancialEventReviewBinding
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyType
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.transactions.domain.model.Category
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FinancialEventReviewBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFinancialEventReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FinancialInboxViewModel by activityViewModels()

    private var event: FinancialEvent? = null
    private var selectedCategoryId: Long = -1L
    private var currentCategories: List<Category> = emptyList()

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            resolvePickedContact(contactUri)
        }
    }

    private val requestContactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(requireContext(), "إذن جهات الاتصال اختياري لتسهيل الاختيار فقط", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFinancialEventReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = arguments?.getLong(ARG_EVENT_ID) ?: -1L
        observeData(eventId)
        setupActions()
    }

    fun setEvent(event: FinancialEvent) {
        this.event = event
    }

    private fun observeData(eventId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pendingEvents.collect { list ->
                        val target = event ?: list.find { it.id == eventId }
                        if (target != null && event == null) {
                            event = target
                            bindEvent(target)
                        }
                    }
                }
                launch {
                    viewModel.categories.collect { cats ->
                        currentCategories = cats
                        event?.let { populateCategoryChips(it.type) }
                    }
                }
            }
        }

        event?.let { bindEvent(it) }
    }

    private fun bindEvent(item: FinancialEvent) {
        val isIncome = item.type == TransactionType.INCOME
        binding.tvReviewAmount.text = Formatters.formatSignedAmount(item.amount, item.type)
        binding.tvReviewAmount.setTextColor(
            if (isIncome) requireContext().getColor(R.color.urwallet_income)
            else requireContext().getColor(R.color.urwallet_expense)
        )

        val cardInfo = if (!item.accountOrCard.isNullOrBlank()) " • ${item.accountOrCard}" else ""
        binding.tvReviewSenderAndDate.text = "من: ${item.sender}$cardInfo • ${DateUtils.formatDateArabic(item.date)}"

        val initialTitle = item.counterparty?.name ?: item.sender
        binding.etTitle.setText(initialTitle)

        item.counterparty?.let { cp ->
            binding.etCounterpartyName.setText(cp.name)
            if (!cp.phoneNumber.isNullOrBlank()) {
                binding.layoutPhoneMapping.isVisible = true
            }
        }

        if (!item.rawMessage.isNullOrBlank()) {
            binding.tvRawMessageQuote.isVisible = true
            binding.tvRawMessageQuote.text = "نص الرسالة: ${item.rawMessage}"
        } else {
            binding.tvRawMessageQuote.isVisible = false
        }

        populateCategoryChips(item.type)
    }

    private fun populateCategoryChips(type: TransactionType) {
        val matchingCategories = currentCategories.filter {
            if (type == TransactionType.INCOME) it.type.name == "INCOME"
            else it.type.name == "EXPENSE"
        }

        binding.cgCategories.removeAllViews()
        for (cat in matchingCategories) {
            val chip = Chip(requireContext()).apply {
                text = cat.name
                isCheckable = true
                id = View.generateViewId()
                tag = cat.id
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategoryId = cat.id
                    }
                }
            }
            binding.cgCategories.addView(chip)

            // Select default or first
            if (selectedCategoryId == cat.id || (selectedCategoryId == -1L && cat.isDefault)) {
                chip.isChecked = true
                selectedCategoryId = cat.id
            }
        }

        if (selectedCategoryId == -1L && matchingCategories.isNotEmpty()) {
            (binding.cgCategories.getChildAt(0) as? Chip)?.isChecked = true
            selectedCategoryId = matchingCategories.first().id
        }
    }

    private fun setupActions() {
        binding.btnPickContact.setOnClickListener {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                contactPickerLauncher.launch(null)
            } else {
                requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        binding.btnConfirmReview.setOnClickListener {
            val currentEvent = event ?: return@setOnClickListener
            if (selectedCategoryId <= 0L) {
                Toast.makeText(requireContext(), R.string.label_select_category, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val title = binding.etTitle.text?.toString()?.trim().orEmpty()
            val note = binding.etNote.text?.toString()?.trim()
            val cpName = binding.etCounterpartyName.text?.toString()?.trim()
            val saveMapping = binding.cbRememberMapping.isChecked

            val updatedCounterparty = if (!cpName.isNullOrBlank()) {
                Counterparty(
                    name = cpName,
                    type = currentEvent.counterparty?.type ?: CounterpartyType.PERSON,
                    phoneNumber = currentEvent.counterparty?.phoneNumber
                )
            } else currentEvent.counterparty

            viewModel.confirmEvent(
                eventId = currentEvent.id,
                categoryId = selectedCategoryId,
                title = title,
                note = note,
                date = currentEvent.date,
                counterparty = updatedCounterparty,
                saveCounterpartyMapping = saveMapping
            )

            dismiss()
        }

        binding.btnCancelReview.setOnClickListener {
            dismiss()
        }
    }

    private fun resolvePickedContact(uri: Uri) {
        val cursor = requireContext().contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIdx != -1) {
                    val name = it.getString(nameIdx)
                    if (!name.isNullOrBlank()) {
                        binding.etCounterpartyName.setText(name)
                        if (binding.etTitle.text.isNullOrBlank()) {
                            binding.etTitle.setText(name)
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
        const val TAG = "FinancialEventReviewBottomSheetFragment"
        private const val ARG_EVENT_ID = "arg_event_id"

        fun newInstance(event: FinancialEvent): FinancialEventReviewBottomSheetFragment {
            return FinancialEventReviewBottomSheetFragment().apply {
                this.event = event
                arguments = Bundle().apply {
                    putLong(ARG_EVENT_ID, event.id)
                }
            }
        }
    }
}

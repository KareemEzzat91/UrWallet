package com.example.urwallet.features.transactions.presentation

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.databinding.FragmentTransactionDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionDetailFragment : Fragment() {

    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeData()
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_transaction_title)
            .setMessage(R.string.dialog_delete_transaction_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteTransaction()
            }
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: TransactionDetailUiState) {
        when (state) {
            is TransactionDetailUiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.scrollDetailContent.isVisible = false
                binding.tvError.isVisible = false
            }
            is TransactionDetailUiState.Success -> {
                binding.progressBar.isVisible = false
                binding.scrollDetailContent.isVisible = true
                binding.tvError.isVisible = false

                bindTransaction(state)
            }
            is TransactionDetailUiState.Deleted -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.transaction_deleted_success),
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
            is TransactionDetailUiState.Error -> {
                binding.progressBar.isVisible = false
                binding.scrollDetailContent.isVisible = false
                binding.tvError.isVisible = true
                binding.tvError.text = state.message
            }
        }
    }

    private fun bindTransaction(state: TransactionDetailUiState.Success) {
        val tx = state.transaction
        val category = state.category

        // 1. Title & Amount
        binding.tvTransactionTitle.text = tx.title
        binding.tvTransactionAmount.text = Formatters.formatSignedAmount(
            amount = tx.amount,
            type = tx.type
        )

        // Amount color
        val amountColor = if (tx.type == TransactionType.INCOME) {
            ContextCompat.getColor(requireContext(), R.color.urwallet_income)
        } else {
            ContextCompat.getColor(requireContext(), R.color.urwallet_expense)
        }
        binding.tvTransactionAmount.setTextColor(amountColor)

        // 2. Type Badge
        val isExpense = tx.type == TransactionType.EXPENSE
        binding.tvTransactionTypeBadge.text = if (isExpense) {
            getString(R.string.label_expense)
        } else {
            getString(R.string.label_income)
        }
        binding.tvTransactionTypeBadge.setTextColor(amountColor)
        binding.tvTransactionTypeBadge.backgroundTintList = ColorStateList.valueOf(
            if (isExpense) Color.parseColor("#FEE2E2") else Color.parseColor("#DCFCE7")
        )

        // 3. Category Icon & Details
        val categoryName = category?.name ?: getString(R.string.cat_other)
        val iconRes = CategoryIconMapper.getIconDrawableRes(category?.icon ?: "ic_other")
        val categoryColor = CategoryIconMapper.parseColorSafely(category?.color ?: "#78909C")

        binding.ivCategoryIcon.setImageResource(iconRes)
        binding.flCategoryIconBadge.backgroundTintList = ColorStateList.valueOf(categoryColor)
        binding.tvCategoryName.text = categoryName

        // 4. Date & Time
        val dateFormatted = DateUtils.formatDateArabic(tx.date)
        val timeFormatted = DateUtils.formatTimeArabic(tx.date)
        binding.tvDateTime.text = "$dateFormatted • $timeFormatted"

        // 5. Note
        binding.tvNote.text = tx.note?.ifBlank { null } ?: getString(R.string.detail_no_note)

        // 6. Share button
        binding.btnShare.setOnClickListener {
            shareTransaction(tx.title, Formatters.formatCurrency(tx.amount), categoryName, "$dateFormatted $timeFormatted")
        }
    }

    private fun shareTransaction(title: String, amount: String, category: String, date: String) {
        val shareText = getString(R.string.share_transaction_format, title, amount, category, date)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, getString(R.string.action_share))
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

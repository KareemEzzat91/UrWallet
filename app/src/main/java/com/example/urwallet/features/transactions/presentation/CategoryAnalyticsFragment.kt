package com.example.urwallet.features.transactions.presentation

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.databinding.FragmentCategoryAnalyticsBinding
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.presentation.adapter.TopTransactionsAdapter
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class CategoryAnalyticsFragment : Fragment() {

    private var _binding: FragmentCategoryAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryAnalyticsViewModel by viewModels()
    private lateinit var topTransactionsAdapter: TopTransactionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        topTransactionsAdapter = TopTransactionsAdapter(
            onTransactionClick = { transaction ->
                findNavController().navigate(
                    R.id.action_categoryAnalyticsFragment_to_transactionDetailFragment,
                    bundleOf("transactionId" to transaction.id)
                )
            }
        )
        binding.rvTopTransactions.adapter = topTransactionsAdapter
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

    private fun renderState(state: CategoryAnalyticsUiState) {
        when (state) {
            is CategoryAnalyticsUiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.scrollContent.isVisible = false
                binding.tvError.isVisible = false
            }
            is CategoryAnalyticsUiState.Empty -> {
                binding.progressBar.isVisible = false
                binding.scrollContent.isVisible = false
                binding.tvError.isVisible = true
                binding.tvError.text = getString(R.string.analytics_no_transactions)
            }
            is CategoryAnalyticsUiState.Error -> {
                binding.progressBar.isVisible = false
                binding.scrollContent.isVisible = false
                binding.tvError.isVisible = true
                binding.tvError.text = state.message
            }
            is CategoryAnalyticsUiState.Success -> {
                binding.progressBar.isVisible = false
                binding.scrollContent.isVisible = true
                binding.tvError.isVisible = false

                populateCategoryChips(state.allCategories, state.analytics.category.id)
                bindAnalytics(state)
            }
        }
    }

    private fun populateCategoryChips(categories: List<Category>, activeCategoryId: Long) {
        binding.chipGroupCategories.removeAllViews()
        for (category in categories) {
            val chip = Chip(requireContext()).apply {
                text = category.name
                isCheckable = true
                isChecked = category.id == activeCategoryId
                setChipBackgroundColorResource(
                    if (category.id == activeCategoryId) R.color.urwallet_primary_light else R.color.urwallet_surface
                )
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (category.id == activeCategoryId) R.color.urwallet_primary else R.color.urwallet_text_primary
                    )
                )
                setOnClickListener {
                    viewModel.selectCategory(category.id)
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun bindAnalytics(state: CategoryAnalyticsUiState.Success) {
        val analytics = state.analytics
        val category = analytics.category

        binding.tvHeaderTitle.text = category.name

        // Total Spent
        binding.tvTotalSpent.text = Formatters.formatCurrency(analytics.totalSpent)

        // Trend Pill
        val pct = analytics.percentageChange
        if (pct != null) {
            binding.layoutTrendBadge.isVisible = true
            val isIncrease = pct > 0
            val formattedPct = "${if (isIncrease) "+" else "-"}${abs(pct).toInt()}%"
            binding.tvTrendPercent.text = formattedPct

            if (isIncrease) {
                // Higher spending -> Red/Expense color
                binding.layoutTrendBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                binding.tvTrendPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.urwallet_expense))
                binding.ivTrendIcon.setImageResource(R.drawable.ic_trending_up)
                binding.ivTrendIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.urwallet_expense))
            } else {
                // Lower spending -> Green/Income color
                binding.layoutTrendBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                binding.tvTrendPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.urwallet_income))
                binding.ivTrendIcon.setImageResource(R.drawable.ic_trending_down)
                binding.ivTrendIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.urwallet_income))
            }
        } else {
            binding.layoutTrendBadge.isVisible = false
        }

        // Average & Count
        binding.tvAverageAmount.text = Formatters.formatCurrency(analytics.averageAmount)
        binding.tvTransactionCount.text = analytics.transactionCount.toString()

        // Insight / Alert Card
        if (!analytics.insightMessage.isNullOrBlank()) {
            binding.cardInsightAlert.isVisible = true
            binding.tvInsightMessage.text = analytics.insightMessage
        } else {
            binding.cardInsightAlert.isVisible = false
        }

        // Top Transactions
        topTransactionsAdapter.category = category
        topTransactionsAdapter.submitList(analytics.topTransactions)
        binding.tvNoTransactions.isVisible = analytics.topTransactions.isEmpty()
        binding.rvTopTransactions.isVisible = analytics.topTransactions.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

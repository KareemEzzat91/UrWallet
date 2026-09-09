package com.example.urwallet.features.analytics.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.FragmentHabitsBinding
import com.example.urwallet.features.analytics.domain.model.FinancialHabitsResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        observeData()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.habitsUiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: HabitsUiState) {
        when (state) {
            is HabitsUiState.Loading -> {
                binding.progressLoading.isVisible = true
                binding.layoutHabitsContent.isVisible = false
            }
            is HabitsUiState.Empty -> {
                binding.progressLoading.isVisible = false
                binding.layoutHabitsContent.isVisible = false
            }
            is HabitsUiState.Error -> {
                binding.progressLoading.isVisible = false
                binding.layoutHabitsContent.isVisible = false
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
            is HabitsUiState.Success -> {
                binding.progressLoading.isVisible = false
                binding.layoutHabitsContent.isVisible = true
                renderHabits(state.result)
            }
        }
    }

    private fun renderHabits(result: FinancialHabitsResult) {
        // Month badge
        binding.tvHabitsMonthBadge.text = "تحليل شهر ${DateUtils.formatMonthYearArabic(result.month, result.year)}"

        // 1. Personality
        val p = result.personality
        binding.tvPersonalityEmoji.text = p.emoji
        binding.tvPersonalityTitle.text = p.title
        binding.tvPersonalityDescription.text = p.description
        binding.tvCoachRecommendation.text = p.coachRecommendation

        // 2. Peak Day
        if (result.peakSpendingDay != null && result.peakSpendingAmount > 0.0) {
            binding.tvPeakDayName.text = result.peakSpendingDay
            binding.tvPeakDayAmount.text = "إجمالي ${Formatters.formatCurrency(result.peakSpendingAmount)}"
        } else {
            binding.tvPeakDayName.text = "لا توجد مصروفات"
            binding.tvPeakDayAmount.text = "0 ج.م"
        }

        // 3. Daily Average
        binding.tvDailyAverageAmount.text = Formatters.formatCurrency(result.dailyAverageSpend)

        // 4. 50/30/20 Rule Breakdown
        val rule = result.rule50_30_20
        binding.tvNeedsAmountAndPct.text = "${Formatters.formatCurrency(rule.needsAmount)} (${String.format(Locale.getDefault(), "%.0f%%", rule.needsPercentage)})"
        binding.progressNeeds.progress = rule.needsPercentage.toInt().coerceIn(0, 100)

        binding.tvWantsAmountAndPct.text = "${Formatters.formatCurrency(rule.wantsAmount)} (${String.format(Locale.getDefault(), "%.0f%%", rule.wantsPercentage)})"
        binding.progressWants.progress = rule.wantsPercentage.toInt().coerceIn(0, 100)

        binding.tvSavingsAmountAndPct.text = "${Formatters.formatCurrency(rule.savingsAmount)} (${String.format(Locale.getDefault(), "%.0f%%", rule.savingsPercentage)})"
        binding.progressSavings.progress = rule.savingsPercentage.toInt().coerceIn(0, 100)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.FragmentHabitsBinding
import com.example.urwallet.features.analytics.domain.model.FinancialHabitsResult
import com.example.urwallet.features.analytics.domain.model.PersonalityType
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
        binding.tvHabitsMonthBadge.text = getString(
            R.string.analytics_habit_badge_format,
            DateUtils.formatMonthYearLocalized(result.month, result.year)
        )

        // 1. Personality
        val p = result.personality
        val (titleRes, descRes, coachRes) = when (p.type) {
            PersonalityType.CAUTIOUS_SAVER -> Triple(
                R.string.personality_cautious_saver_title,
                R.string.personality_cautious_saver_desc,
                R.string.personality_cautious_saver_coach
            )
            PersonalityType.SMART_PLANNER -> Triple(
                R.string.personality_smart_planner_title,
                R.string.personality_smart_planner_desc,
                R.string.personality_smart_planner_coach
            )
            PersonalityType.SPONTANEOUS_SPENDER -> Triple(
                R.string.personality_spontaneous_spender_title,
                R.string.personality_spontaneous_spender_desc,
                R.string.personality_spontaneous_spender_coach
            )
            PersonalityType.BALANCED -> Triple(
                R.string.personality_balanced_title,
                R.string.personality_balanced_desc,
                R.string.personality_balanced_coach
            )
        }
        binding.tvPersonalityEmoji.text = p.emoji
        binding.tvPersonalityTitle.text = getString(titleRes)
        binding.tvPersonalityDescription.text = getString(descRes)
        binding.tvCoachRecommendation.text = getString(coachRes)

        // 2. Peak Day
        if (result.peakSpendingDay != null && result.peakSpendingAmount > 0.0) {
            val localizedDay = when (result.peakSpendingDay) {
                "السبت" -> if (Locale.getDefault().language == "en") "Saturday" else "السبت"
                "الأحد" -> if (Locale.getDefault().language == "en") "Sunday" else "الأحد"
                "الاثنين" -> if (Locale.getDefault().language == "en") "Monday" else "الاثنين"
                "الثلاثاء" -> if (Locale.getDefault().language == "en") "Tuesday" else "الثلاثاء"
                "الأربعاء" -> if (Locale.getDefault().language == "en") "Wednesday" else "الأربعاء"
                "الخميس" -> if (Locale.getDefault().language == "en") "Thursday" else "الخميس"
                "الجمعة" -> if (Locale.getDefault().language == "en") "Friday" else "الجمعة"
                else -> result.peakSpendingDay
            }
            binding.tvPeakDayName.text = localizedDay
            binding.tvPeakDayAmount.text = getString(
                R.string.habits_peak_total_format,
                Formatters.formatCurrency(result.peakSpendingAmount)
            )
        } else {
            binding.tvPeakDayName.text = getString(R.string.habits_no_expenses)
            binding.tvPeakDayAmount.text = Formatters.formatCurrency(0.0)
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

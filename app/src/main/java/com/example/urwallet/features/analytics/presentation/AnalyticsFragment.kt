package com.example.urwallet.features.analytics.presentation

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.FragmentAnalyticsBinding
import com.example.urwallet.features.analytics.domain.model.HealthRating
import com.example.urwallet.features.analytics.domain.model.MonthlyAnalyticsResult
import com.example.urwallet.features.analytics.presentation.adapter.CategorySpendingAdapter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategorySpendingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupMonthNavigator()
        setupCategoryList()
        setupSpendingChart()
        setupHabitsNavigation()
        observeData()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupMonthNavigator() {
        binding.btnPrevMonth.setOnClickListener {
            viewModel.selectPreviousMonth()
        }
        binding.btnNextMonth.setOnClickListener {
            viewModel.selectNextMonth()
        }
    }

    private fun setupCategoryList() {
        categoryAdapter = CategorySpendingAdapter { item ->
            findNavController().navigate(
                R.id.action_analyticsFragment_to_categoryAnalyticsFragment,
                bundleOf("categoryId" to item.categoryId)
            )
        }
        binding.rvCategorySpending.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategorySpending.adapter = categoryAdapter
    }

    private fun setupSpendingChart() {
        val chart = binding.barChartSpending
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = requireContext().getColor(R.color.urwallet_text_secondary)
        xAxis.textSize = 10f
        xAxis.granularity = 5f
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val axisLeft = chart.axisLeft
        axisLeft.setDrawGridLines(true)
        axisLeft.gridColor = Color.parseColor("#14000000")
        axisLeft.textColor = requireContext().getColor(R.color.urwallet_text_secondary)
        axisLeft.textSize = 10f
        axisLeft.axisMinimum = 0f
        axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()
            }
        }

        chart.axisRight.isEnabled = false
    }

    private fun setupHabitsNavigation() {
        val openHabits = {
            findNavController().navigate(R.id.action_analyticsFragment_to_habitsFragment)
        }
        binding.btnNavHabits.setOnClickListener { openHabits() }
        binding.cardHabitsBanner.setOnClickListener { openHabits() }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedMonth.collect { month ->
                        updateMonthTitle(month, viewModel.selectedYear.value)
                    }
                }
                launch {
                    viewModel.selectedYear.collect { year ->
                        updateMonthTitle(viewModel.selectedMonth.value, year)
                    }
                }
                launch {
                    viewModel.analyticsUiState.collect { state ->
                        renderState(state)
                    }
                }
            }
        }
    }

    private fun updateMonthTitle(month: Int, year: Int) {
        binding.tvSelectedMonthYear.text = DateUtils.formatMonthYearArabic(month, year)
    }

    private fun renderState(state: AnalyticsUiState) {
        when (state) {
            is AnalyticsUiState.Loading -> {
                binding.progressLoading.isVisible = true
                binding.layoutAnalyticsContent.isVisible = false
                binding.layoutEmptyState.isVisible = false
            }
            is AnalyticsUiState.Empty -> {
                binding.progressLoading.isVisible = false
                binding.layoutAnalyticsContent.isVisible = false
                binding.layoutEmptyState.isVisible = true
            }
            is AnalyticsUiState.Error -> {
                binding.progressLoading.isVisible = false
                binding.layoutAnalyticsContent.isVisible = false
                binding.layoutEmptyState.isVisible = false
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
            is AnalyticsUiState.Success -> {
                binding.progressLoading.isVisible = false
                binding.layoutEmptyState.isVisible = false
                binding.layoutAnalyticsContent.isVisible = true
                renderAnalytics(state.result)
            }
        }
    }

    private fun renderAnalytics(result: MonthlyAnalyticsResult) {
        val context = requireContext()

        // 1. Health Score
        val hs = result.healthScore
        binding.tvHealthScoreValue.text = hs.overallScore.toString()
        binding.tvHealthRatingBadge.text = when (hs.rating) {
            HealthRating.EXCELLENT -> "ممتاز"
            HealthRating.VERY_GOOD -> "جيد جداً"
            HealthRating.AVERAGE -> "متوسط"
            HealthRating.NEEDS_IMPROVEMENT -> "يحتاج تحسين"
        }
        binding.tvHealthCoachingMessage.text = hs.coachingMessage
        binding.tvPlanningScore.text = "${hs.planningScore}%"
        binding.tvSavingScore.text = "${hs.savingScore}%"
        binding.tvControlScore.text = "${hs.controlScore}%"

        // 2. Cash Flow
        binding.tvTotalIncome.text = "+ ${Formatters.formatCurrency(result.totalIncome)}"
        binding.tvTotalExpenses.text = "- ${Formatters.formatCurrency(result.totalExpenses)}"

        val netSavings = result.netSavings
        val isNetPositive = netSavings >= 0.0
        val sign = if (isNetPositive) "+ " else ""
        binding.tvNetSavings.text = "$sign${Formatters.formatCurrency(netSavings)}"
        val netColor = if (isNetPositive) context.getColor(R.color.urwallet_income) else context.getColor(R.color.urwallet_expense)
        binding.tvNetSavings.setTextColor(netColor)

        // 3. Spending Chart
        val entries = ArrayList<BarEntry>()
        var hasChartData = false
        result.dailySpending.forEach { (day, amount) ->
            entries.add(BarEntry(day.toFloat(), amount.toFloat()))
            if (amount > 0.0) hasChartData = true
        }

        if (hasChartData) {
            binding.barChartSpending.isVisible = true
            binding.tvChartEmptyNotice.isVisible = false

            val dataSet = BarDataSet(entries, "المصروفات اليومية").apply {
                color = context.getColor(R.color.urwallet_primary)
                setDrawValues(false)
                highLightColor = Color.TRANSPARENT
            }
            val barData = BarData(dataSet).apply {
                barWidth = 0.6f
            }
            binding.barChartSpending.data = barData
            binding.barChartSpending.invalidate()
        } else {
            binding.barChartSpending.isVisible = false
            binding.tvChartEmptyNotice.isVisible = true
        }

        // 4. Category Breakdown
        val categories = result.categoryBreakdown
        binding.layoutCategoryBreakdownSection.isVisible = categories.isNotEmpty()
        if (categories.isNotEmpty()) {
            binding.tvCategoryBreakdownCount.text = categories.size.toString()
            categoryAdapter.submitList(categories)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.urwallet.features.analytics.presentation

import android.graphics.Color
import android.graphics.Typeface
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
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.databinding.FragmentAnalyticsBinding
import com.example.urwallet.features.analytics.domain.model.AnalyticsTimePeriod
import com.example.urwallet.features.analytics.domain.model.HealthRating
import com.example.urwallet.features.analytics.domain.model.MonthlyAnalyticsResult
import com.example.urwallet.features.analytics.presentation.adapter.CategorySpendingAdapter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
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
        setupPeriodSelector()
        setupMonthNavigator()
        setupCategoryList()
        setupIncomeExpenseChart()
        setupSpendingTrendChart()
        setupCategoryPieChart()
        setupHabitsNavigation()
        observeData()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupPeriodSelector() {
        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selectedPeriod = when (checkedIds.first()) {
                R.id.chipThisMonth -> AnalyticsTimePeriod.THIS_MONTH
                R.id.chipLastMonth -> AnalyticsTimePeriod.LAST_MONTH
                R.id.chipLast3Months -> AnalyticsTimePeriod.LAST_3_MONTHS
                R.id.chipLast6Months -> AnalyticsTimePeriod.LAST_6_MONTHS
                R.id.chipThisYear -> AnalyticsTimePeriod.THIS_YEAR
                else -> AnalyticsTimePeriod.THIS_MONTH
            }
            viewModel.selectTimePeriod(selectedPeriod)
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

    private fun setupIncomeExpenseChart() {
        val chart = binding.barChartIncomeExpense
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.legend.textColor = requireContext().getColor(R.color.urwallet_text_secondary)
        chart.setTouchEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = requireContext().getColor(R.color.urwallet_text_secondary)
        xAxis.textSize = 10f
        xAxis.granularity = 1f

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

    private fun setupSpendingTrendChart() {
        val chart = binding.lineChartSpendingTrend
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setDrawGridBackground(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = requireContext().getColor(R.color.urwallet_text_secondary)
        xAxis.textSize = 10f
        xAxis.granularity = 1f

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

    private fun setupCategoryPieChart() {
        val chart = binding.pieChartCategories
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)
        chart.isDrawHoleEnabled = true
        chart.holeRadius = 58f
        chart.transparentCircleRadius = 62f
        chart.setHoleColor(Color.TRANSPARENT)
        chart.setDrawEntryLabels(false)
        chart.setCenterTextSize(12f)
        chart.setCenterTextColor(requireContext().getColor(R.color.urwallet_text_primary))
        chart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)
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
                    viewModel.selectedTimePeriod.collect { period ->
                        updatePeriodUI(period)
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

    private fun updatePeriodUI(period: AnalyticsTimePeriod) {
        val chipId = when (period) {
            AnalyticsTimePeriod.THIS_MONTH -> R.id.chipThisMonth
            AnalyticsTimePeriod.LAST_MONTH -> R.id.chipLastMonth
            AnalyticsTimePeriod.LAST_3_MONTHS -> R.id.chipLast3Months
            AnalyticsTimePeriod.LAST_6_MONTHS -> R.id.chipLast6Months
            AnalyticsTimePeriod.THIS_YEAR -> R.id.chipThisYear
        }
        if (binding.chipGroupPeriod.checkedChipId != chipId) {
            binding.chipGroupPeriod.check(chipId)
        }
        // Month navigator is visible for single-month views
        binding.cardMonthNavigator.isVisible = (period == AnalyticsTimePeriod.THIS_MONTH || period == AnalyticsTimePeriod.LAST_MONTH)
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

        if (result.totalGoalSavings > 0.0) {
            binding.layoutGoalSavings.isVisible = true
            binding.tvGoalSavings.text = Formatters.formatCurrency(result.totalGoalSavings)
        } else {
            binding.layoutGoalSavings.isVisible = false
        }

        // 3. Income vs Expense Comparison Bar Chart
        renderIncomeExpenseChart(result)

        // 4. Daily Spending Trend Line Chart
        renderSpendingTrendChart(result)

        // 5. Category Breakdown Pie / Donut Chart
        renderCategoryPieChart(result)

        // 6. Ranked Top Spending Categories List
        val categories = result.categoryBreakdown
        binding.layoutCategoryBreakdownSection.isVisible = categories.isNotEmpty()
        if (categories.isNotEmpty()) {
            binding.tvCategoryBreakdownCount.text = categories.size.toString()
            categoryAdapter.submitList(categories)
        }
    }

    private fun renderIncomeExpenseChart(result: MonthlyAnalyticsResult) {
        val context = requireContext()
        val cashFlows = result.cashFlowComparison
        val hasData = result.totalIncome > 0.0 || result.totalExpenses > 0.0

        if (hasData && cashFlows.isNotEmpty()) {
            binding.barChartIncomeExpense.isVisible = true
            binding.tvIncomeExpenseChartEmpty.isVisible = false

            val incomeEntries = ArrayList<BarEntry>()
            val expenseEntries = ArrayList<BarEntry>()

            cashFlows.forEachIndexed { index, point ->
                incomeEntries.add(BarEntry(index.toFloat(), point.income.toFloat()))
                expenseEntries.add(BarEntry(index.toFloat(), point.expense.toFloat()))
            }

            val incomeSet = BarDataSet(incomeEntries, "الدخل").apply {
                color = context.getColor(R.color.urwallet_income)
                setDrawValues(false)
            }
            val expenseSet = BarDataSet(expenseEntries, "المصروف").apply {
                color = context.getColor(R.color.urwallet_expense)
                setDrawValues(false)
            }

            val groupSpace = 0.28f
            val barSpace = 0.06f
            val barWidth = 0.30f

            val barData = BarData(incomeSet, expenseSet).apply {
                this.barWidth = barWidth
            }

            val chart = binding.barChartIncomeExpense
            chart.data = barData
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = cashFlows.size.toFloat()
            chart.groupBars(0f, groupSpace, barSpace)

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val idx = value.toInt()
                    return if (idx in cashFlows.indices) {
                        cashFlows[idx].monthName.split(" ").firstOrNull() ?: cashFlows[idx].monthName
                    } else ""
                }
            }

            chart.invalidate()
        } else {
            binding.barChartIncomeExpense.isVisible = false
            binding.tvIncomeExpenseChartEmpty.isVisible = true
        }
    }

    private fun renderSpendingTrendChart(result: MonthlyAnalyticsResult) {
        val context = requireContext()
        val points = result.dailyExpensePoints.ifEmpty {
            result.dailySpending.map { (day, amount) ->
                com.example.urwallet.features.analytics.domain.model.DailyExpensePoint(
                    date = day.toLong(),
                    dayNumber = day,
                    label = "$day",
                    amount = amount
                )
            }
        }

        val hasSpending = points.any { it.amount > 0.0 }

        if (hasSpending) {
            binding.lineChartSpendingTrend.isVisible = true
            binding.tvChartEmptyNotice.isVisible = false

            val entries = points.mapIndexed { index, pt ->
                Entry(index.toFloat(), pt.amount.toFloat())
            }

            val dataSet = LineDataSet(entries, "المصروفات").apply {
                color = context.getColor(R.color.urwallet_primary)
                lineWidth = 2.5f
                setCircleColor(context.getColor(R.color.urwallet_primary))
                circleRadius = 3.5f
                setDrawCircleHole(true)
                circleHoleRadius = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = context.getColor(R.color.urwallet_primary)
                fillAlpha = 35
                setDrawValues(false)
            }

            binding.lineChartSpendingTrend.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val idx = value.toInt()
                    return if (idx in points.indices) points[idx].label else ""
                }
            }

            binding.lineChartSpendingTrend.data = LineData(dataSet)
            binding.lineChartSpendingTrend.invalidate()
        } else {
            binding.lineChartSpendingTrend.isVisible = false
            binding.tvChartEmptyNotice.isVisible = true
        }
    }

    private fun renderCategoryPieChart(result: MonthlyAnalyticsResult) {
        val categories = result.categoryBreakdown
        if (categories.isNotEmpty() && result.totalExpenses > 0.0) {
            binding.cardCategoryPieChart.isVisible = true
            binding.pieChartCategories.isVisible = true
            binding.tvPieChartEmptyNotice.isVisible = false

            val pieEntries = categories.map {
                PieEntry(it.amount.toFloat(), it.categoryName)
            }
            val pieColors = categories.map {
                CategoryIconMapper.parseColorSafely(it.categoryColor)
            }

            val dataSet = PieDataSet(pieEntries, "توزيع المصروفات").apply {
                colors = pieColors
                sliceSpace = 2f
                setDrawValues(false)
            }

            binding.pieChartCategories.centerText = "المصروفات\n${Formatters.formatCurrency(result.totalExpenses)}"
            binding.pieChartCategories.data = PieData(dataSet)
            binding.pieChartCategories.invalidate()
        } else {
            binding.pieChartCategories.isVisible = false
            binding.tvPieChartEmptyNotice.isVisible = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

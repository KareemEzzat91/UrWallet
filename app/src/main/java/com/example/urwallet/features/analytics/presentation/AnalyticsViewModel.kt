package com.example.urwallet.features.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.features.analytics.domain.model.FinancialHabitsResult
import com.example.urwallet.features.analytics.domain.model.MonthlyAnalyticsResult
import com.example.urwallet.features.analytics.domain.usecase.GetFinancialHabitsUseCase
import com.example.urwallet.features.analytics.domain.usecase.GetMonthlyAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getMonthlyAnalyticsUseCase: GetMonthlyAnalyticsUseCase,
    private val getFinancialHabitsUseCase: GetFinancialHabitsUseCase
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(DateUtils.getCurrentMonth())
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(DateUtils.getCurrentYear())
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val selectedPeriod = MutableStateFlow(Pair(_selectedMonth.value, _selectedYear.value))

    val analyticsUiState: StateFlow<AnalyticsUiState> = selectedPeriod
        .flatMapLatest { (month, year) ->
            getMonthlyAnalyticsUseCase(month, year)
                .map<MonthlyAnalyticsResult, AnalyticsUiState> { result ->
                    if (!result.hasData && result.categoryBreakdown.isEmpty()) {
                        AnalyticsUiState.Empty
                    } else {
                        AnalyticsUiState.Success(result)
                    }
                }
                .catch { e -> emit(AnalyticsUiState.Error(e.localizedMessage ?: "حدث خطأ أثناء تحميل التحليلات")) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AnalyticsUiState.Loading
        )

    val habitsUiState: StateFlow<HabitsUiState> = selectedPeriod
        .flatMapLatest { (month, year) ->
            getFinancialHabitsUseCase(month, year)
                .map<FinancialHabitsResult, HabitsUiState> { result ->
                    HabitsUiState.Success(result)
                }
                .catch { e -> emit(HabitsUiState.Error(e.localizedMessage ?: "حدث خطأ أثناء تحميل العادات المالية")) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HabitsUiState.Loading
        )

    fun selectPreviousMonth() {
        val (prevM, prevY) = DateUtils.getPreviousMonth(_selectedMonth.value, _selectedYear.value)
        _selectedMonth.value = prevM
        _selectedYear.value = prevY
        selectedPeriod.value = Pair(prevM, prevY)
    }

    fun selectNextMonth() {
        val (nextM, nextY) = DateUtils.getNextMonth(_selectedMonth.value, _selectedYear.value)
        _selectedMonth.value = nextM
        _selectedYear.value = nextY
        selectedPeriod.value = Pair(nextM, nextY)
    }

    fun selectCurrentMonth() {
        val m = DateUtils.getCurrentMonth()
        val y = DateUtils.getCurrentYear()
        _selectedMonth.value = m
        _selectedYear.value = y
        selectedPeriod.value = Pair(m, y)
    }
}

package com.example.urwallet.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.dashboard.domain.usecase.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase
) : ViewModel() {

    val dashboardUiState: StateFlow<DashboardUiState> = getDashboardSummaryUseCase()
        .map<_, DashboardUiState> { summary ->
            DashboardUiState.Success(summary)
        }
        .catch { error ->
            emit(DashboardUiState.Error(error.message ?: "حدث خطأ أثناء تحميل لوحة التحكم"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
}

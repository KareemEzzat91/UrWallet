package com.example.urwallet.features.budgets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.features.budgets.domain.usecase.DeleteBudgetUseCase
import com.example.urwallet.features.budgets.domain.usecase.GetBudgetsSummaryUseCase
import com.example.urwallet.features.budgets.domain.usecase.SaveBudgetUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    getBudgetsSummaryUseCase: GetBudgetsSummaryUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val budgetsUiState: StateFlow<BudgetsUiState> = getBudgetsSummaryUseCase()
        .map { result ->
            if (result.isEmpty) {
                BudgetsUiState.Empty
            } else {
                BudgetsUiState.Success(result)
            }
        }
        .catch { e -> emit(BudgetsUiState.Error(e.localizedMessage ?: "حدث خطأ أثناء تحميل الميزانيات")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = BudgetsUiState.Loading
        )

    val expenseCategories: StateFlow<List<Category>> = categoryRepository
        .getCategoriesByType(CategoryType.EXPENSE)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    suspend fun saveBudget(
        categoryId: Long?,
        amount: Double,
        month: Int = DateUtils.getCurrentMonth(),
        year: Int = DateUtils.getCurrentYear(),
        alertThreshold: Double = 0.80
    ): Result<Long> {
        return saveBudgetUseCase(
            categoryId = categoryId,
            amount = amount,
            month = month,
            year = year,
            alertThreshold = alertThreshold
        )
    }

    fun deleteBudget(budgetId: Long, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = deleteBudgetUseCase(budgetId)
            onResult(result)
        }
    }
}

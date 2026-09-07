package com.example.urwallet.features.transactions.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.transactions.domain.usecase.GetCategoriesUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetCategoryAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryAnalyticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCategoryAnalyticsUseCase: GetCategoryAnalyticsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val initialCategoryId: Long = savedStateHandle.get<Long>("categoryId") ?: -1L
    private val _selectedCategoryId = MutableStateFlow(initialCategoryId)
    val selectedCategoryId: StateFlow<Long> = _selectedCategoryId.asStateFlow()

    val uiState: StateFlow<CategoryAnalyticsUiState> = getCategoriesUseCase().flatMapLatest { allCategories ->
        if (allCategories.isEmpty()) {
            flowOf(CategoryAnalyticsUiState.Empty)
        } else {
            val targetId = if (_selectedCategoryId.value > 0 && allCategories.any { it.id == _selectedCategoryId.value }) {
                _selectedCategoryId.value
            } else {
                val firstId = allCategories.first().id
                _selectedCategoryId.value = firstId
                firstId
            }

            _selectedCategoryId.flatMapLatest { currentSelectedId ->
                getCategoryAnalyticsUseCase(currentSelectedId).combine(flowOf(allCategories)) { analytics, cats ->
                    if (analytics == null) {
                        CategoryAnalyticsUiState.Empty
                    } else {
                        CategoryAnalyticsUiState.Success(
                            analytics = analytics,
                            allCategories = cats
                        )
                    }
                }
            }
        }
    }.catch { error ->
        emit(CategoryAnalyticsUiState.Error(error.message ?: "حدث خطأ أثناء تحميل تحليل الفئة"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryAnalyticsUiState.Loading
    )

    fun selectCategory(categoryId: Long) {
        if (_selectedCategoryId.value != categoryId) {
            _selectedCategoryId.value = categoryId
        }
    }
}

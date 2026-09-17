package com.example.urwallet.features.more.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.usecase.AddCategoryUseCase
import com.example.urwallet.features.transactions.domain.usecase.DeleteCategoryUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetCategoriesUseCase
import com.example.urwallet.features.transactions.domain.usecase.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val selectedType: CategoryType = CategoryType.EXPENSE,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun setTypeFilter(type: CategoryType) {
        _uiState.update { it.copy(selectedType = type) }
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getCategoriesUseCase(uiState.value.selectedType)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "خطأ في تحميل التصنيفات")
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(categories = list, isLoading = false)
                    }
                }
        }
    }

    fun saveCategory(
        id: Long,
        name: String,
        type: CategoryType,
        icon: String,
        color: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (id > 0) {
                updateCategoryUseCase(id, name, type, icon, color)
            } else {
                addCategoryUseCase(name, type, icon, color).map { }
            }

            result.fold(
                onSuccess = {
                    val msg = if (id > 0) "تم تعديل التصنيف بنجاح" else "تمت إضافة التصنيف بنجاح"
                    _uiState.update { it.copy(isLoading = false, userMessage = msg, errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "حدث خطأ أثناء حفظ التصنيف") }
                }
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = deleteCategoryUseCase(category.id)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, userMessage = "تم حذف التصنيف بنجاح", errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "تعذر حذف التصنيف") }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }
}

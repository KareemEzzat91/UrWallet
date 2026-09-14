package com.example.urwallet.features.more.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.more.domain.usecase.AddRecurringTransactionUseCase
import com.example.urwallet.features.more.domain.usecase.DeleteRecurringTransactionUseCase
import com.example.urwallet.features.more.domain.usecase.GetRecurringTransactionsUseCase
import com.example.urwallet.features.more.domain.usecase.ProcessDueRecurringTransactionsUseCase
import com.example.urwallet.features.more.domain.usecase.ToggleRecurringTransactionUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.usecase.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val addRecurringTransactionUseCase: AddRecurringTransactionUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val toggleRecurringTransactionUseCase: ToggleRecurringTransactionUseCase,
    private val processDueRecurringTransactionsUseCase: ProcessDueRecurringTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _filterType = MutableStateFlow(RecurringFilterType.ALL)

    private val _events = MutableSharedFlow<RecurringUiEvent>()
    val events: SharedFlow<RecurringUiEvent> = _events.asSharedFlow()

    private val _selectedTypeForAdd = MutableStateFlow(TransactionType.EXPENSE)
    val selectedTypeForAdd: StateFlow<TransactionType> = _selectedTypeForAdd.asStateFlow()

    val categoriesForAdd: StateFlow<List<Category>> = _selectedTypeForAdd.flatMapLatest { type ->
        val categoryType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        getCategoriesUseCase(categoryType)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<RecurringUiState> = combine(
        getRecurringTransactionsUseCase(),
        _filterType
    ) { summary, filter ->
        val filtered = when (filter) {
            RecurringFilterType.ALL -> summary.items
            RecurringFilterType.EXPENSE -> summary.items.filter { it.recurring.type == TransactionType.EXPENSE }
            RecurringFilterType.INCOME -> summary.items.filter { it.recurring.type == TransactionType.INCOME }
        }

        RecurringUiState(
            isLoading = false,
            items = summary.items,
            filteredItems = filtered,
            monthlyObligations = summary.monthlyObligations,
            monthlyRecurringIncome = summary.monthlyRecurringIncome,
            activeSubscriptionsCount = summary.activeSubscriptionsCount,
            filterType = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringUiState()
    )

    init {
        // Trigger due transactions evaluation when entering recurring hub
        viewModelScope.launch {
            try {
                processDueRecurringTransactionsUseCase()
            } catch (e: Exception) {
                // Background safe
            }
        }
    }

    fun setFilter(filter: RecurringFilterType) {
        _filterType.value = filter
    }

    fun setSelectedTypeForAdd(type: TransactionType) {
        _selectedTypeForAdd.value = type
    }

    fun toggleActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            try {
                toggleRecurringTransactionUseCase(id, isActive)
            } catch (e: Exception) {
                _events.emit(RecurringUiEvent.ShowMessage(e.message ?: "حدث خطأ أثناء تغيير الحالة"))
            }
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            try {
                deleteRecurringTransactionUseCase(id)
                _events.emit(RecurringUiEvent.ShowMessage("تم حذف المعاملة المتكررة"))
            } catch (e: Exception) {
                _events.emit(RecurringUiEvent.ShowMessage(e.message ?: "حدث خطأ أثناء الحذف"))
            }
        }
    }

    fun addRecurring(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        frequency: Frequency,
        startDate: Long,
        endDate: Long?
    ) {
        viewModelScope.launch {
            try {
                addRecurringTransactionUseCase(
                    title = title,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate
                )
                _events.emit(RecurringUiEvent.ShowMessage("تمت إضافة المعاملة المتكررة بنجاح"))
                _events.emit(RecurringUiEvent.DismissAddSheet)
            } catch (e: Exception) {
                _events.emit(RecurringUiEvent.ShowMessage(e.message ?: "حدث خطأ أثناء الإضافة"))
            }
        }
    }
}

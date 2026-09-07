package com.example.urwallet.features.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.model.TransactionFilterCriteria
import com.example.urwallet.features.transactions.domain.usecase.AddTransactionUseCase
import com.example.urwallet.features.transactions.domain.usecase.DeleteTransactionUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetCategoriesUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetFilteredTransactionsUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getFilteredTransactionsUseCase: GetFilteredTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    // --- Search & Filter State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCriteria = MutableStateFlow(TransactionFilterCriteria())
    val filterCriteria: StateFlow<TransactionFilterCriteria> = _filterCriteria.asStateFlow()

    val allCategories: StateFlow<List<Category>> = getCategoriesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val debouncedSearchQuery = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()

    // --- Transactions List State ---
    val transactionsUiState: StateFlow<TransactionsUiState> = combine(
        getTransactionsUseCase(),
        getCategoriesUseCase(),
        debouncedSearchQuery,
        _filterCriteria
    ) { allTransactions, categories, query, criteria ->
        if (allTransactions.isEmpty()) {
            TransactionsUiState.Empty
        } else {
            val categoryMap = categories.associateBy { it.id }
            val effectiveCriteria = criteria.copy(query = query)
            val filteredTransactions = getFilteredTransactionsUseCase(
                transactions = allTransactions,
                categoryMap = categoryMap,
                criteria = effectiveCriteria
            )

            if (filteredTransactions.isEmpty()) {
                TransactionsUiState.NoSearchResults(
                    query = query,
                    hasActiveFilters = effectiveCriteria.hasActiveFilters()
                )
            } else {
                val income = filteredTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val expense = filteredTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                val groupedItems = buildGroupedList(filteredTransactions, categoryMap)
                TransactionsUiState.Success(
                    items = groupedItems,
                    totalIncome = income,
                    totalExpense = expense,
                    hasActiveFilters = effectiveCriteria.hasActiveFilters(),
                    activeFilterCount = effectiveCriteria.activeFilterCount()
                )
            }
        }
    }.catch { error ->
        emit(TransactionsUiState.Error(error.message ?: "حدث خطأ أثناء تحميل المعاملات"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState.Loading
    )

    // --- Search & Filter Functions ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setQuickTypeFilter(type: TransactionType?) {
        _filterCriteria.update { current ->
            val newType = if (current.type == type) null else type
            current.copy(type = newType)
        }
    }

    fun applyFilterCriteria(criteria: TransactionFilterCriteria) {
        _filterCriteria.value = criteria.copy(query = _searchQuery.value)
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _filterCriteria.value = TransactionFilterCriteria()
    }

    // --- Add Transaction Form State ---
    // TODO(architecture): isSaved is a boolean state field, not a true one-time event.
    //   This works correctly in practice because resetAddTransactionState() is called
    //   immediately in the Fragment after dismiss(). For future cleanup, replace with
    //   a Channel<UiEffect> pattern (e.g. SharedFlow) to guarantee exactly-once delivery.
    private val _addTransactionUiState = MutableStateFlow(AddTransactionUiState())
    val addTransactionUiState: StateFlow<AddTransactionUiState> = _addTransactionUiState.asStateFlow()

    private var categoriesJob: kotlinx.coroutines.Job? = null

    init {
        loadCategoriesForCurrentType()
    }

    fun selectType(type: TransactionType) {
        if (_addTransactionUiState.value.selectedType == type) return
        _addTransactionUiState.update {
            it.copy(
                selectedType = type,
                selectedCategoryId = null,
                errorMessage = null
            )
        }
        loadCategoriesForCurrentType()
    }

    private fun loadCategoriesForCurrentType() {
        val categoryType = when (_addTransactionUiState.value.selectedType) {
            TransactionType.EXPENSE -> CategoryType.EXPENSE
            TransactionType.INCOME -> CategoryType.INCOME
        }
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            getCategoriesUseCase(categoryType).collect { categories ->
                _addTransactionUiState.update { currentState ->
                    val currentIdValid = categories.any { it.id == currentState.selectedCategoryId }
                    val validSelectedId = if (currentIdValid) currentState.selectedCategoryId else categories.firstOrNull()?.id
                    currentState.copy(
                        categories = categories,
                        selectedCategoryId = validSelectedId
                    )
                }
            }
        }
    }

    fun selectCategory(categoryId: Long) {
        _addTransactionUiState.update {
            it.copy(selectedCategoryId = categoryId, errorMessage = null)
        }
    }

    fun saveTransaction(amountStr: String, title: String, note: String?) {
        val amount = amountStr.trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _addTransactionUiState.update { it.copy(errorMessage = "يرجى إدخال مبلغ صحيح أكبر من صفر") }
            return
        }
        if (title.trim().isBlank()) {
            _addTransactionUiState.update { it.copy(errorMessage = "يرجى إدخال اسم المعاملة") }
            return
        }
        val categoryId = _addTransactionUiState.value.selectedCategoryId
        if (categoryId == null || categoryId <= 0L) {
            _addTransactionUiState.update { it.copy(errorMessage = "يرجى اختيار تصنيف للمعاملة") }
            return
        }

        _addTransactionUiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = addTransactionUseCase(
                amount = amount,
                type = _addTransactionUiState.value.selectedType,
                categoryId = categoryId,
                title = title.trim(),
                note = note?.trim()?.ifBlank { null }
            )

            result.fold(
                onSuccess = {
                    _addTransactionUiState.update {
                        it.copy(isLoading = false, isSaved = true, errorMessage = null)
                    }
                },
                onFailure = { error ->
                    _addTransactionUiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "فشل حفظ المعاملة")
                    }
                }
            )
        }
    }

    fun resetAddTransactionState() {
        _addTransactionUiState.update {
            AddTransactionUiState(
                selectedType = TransactionType.EXPENSE,
                selectedCategoryId = null,
                isLoading = false,
                isSaved = false,
                errorMessage = null
            )
        }
        loadCategoriesForCurrentType()
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
        }
    }

    private fun buildGroupedList(
        transactions: List<Transaction>,
        categoryMap: Map<Long, Category>
    ): List<TransactionListItem> {
        val items = mutableListOf<TransactionListItem>()
        val groupedByDate = transactions.groupBy { DateUtils.formatDateArabic(it.date) }

        for ((dateLabel, dayTransactions) in groupedByDate) {
            items.add(TransactionListItem.Header(dateLabel))
            for (transaction in dayTransactions) {
                val category = categoryMap[transaction.categoryId]
                items.add(TransactionListItem.Item(transaction, category))
            }
        }
        return items
    }
}

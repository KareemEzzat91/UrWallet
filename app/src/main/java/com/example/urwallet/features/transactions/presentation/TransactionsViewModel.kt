package com.example.urwallet.features.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.model.TransactionFilterCriteria
import com.example.urwallet.features.notifications.domain.usecase.CheckBudgetAlertUseCase
import com.example.urwallet.features.transactions.domain.usecase.AddTransactionUseCase
import com.example.urwallet.features.transactions.domain.usecase.BulkChangeCategoryUseCase
import com.example.urwallet.features.transactions.domain.usecase.BulkDeleteTransactionsUseCase
import com.example.urwallet.features.transactions.domain.usecase.DeleteTransactionUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetCategoriesUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetFilteredTransactionsUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetTransactionsUseCase
import com.example.urwallet.features.transactions.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getFilteredTransactionsUseCase: GetFilteredTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val bulkDeleteTransactionsUseCase: BulkDeleteTransactionsUseCase,
    private val bulkChangeCategoryUseCase: BulkChangeCategoryUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val checkBudgetAlertUseCase: CheckBudgetAlertUseCase
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

    // --- Multi-Selection State ---
    private val _selectedTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<Long>> = _selectedTransactionIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _isBulkOperating = MutableStateFlow(false)
    val isBulkOperating: StateFlow<Boolean> = _isBulkOperating.asStateFlow()

    // --- Transactions List State ---
    val transactionsUiState: StateFlow<TransactionsUiState> = combine(
        debouncedSearchQuery,
        _filterCriteria
    ) { query, criteria ->
        criteria.copy(query = query)
    }.flatMapLatest { effectiveCriteria ->
        combine(
            getFilteredTransactionsUseCase(effectiveCriteria),
            getCategoriesUseCase(),
            _selectedTransactionIds,
            _isSelectionMode
        ) { filteredTransactions, categories, selectedIds, isSelectionMode ->
            val categoryMap = categories.associateBy { it.id }

            if (filteredTransactions.isEmpty()) {
                if (effectiveCriteria.hasActiveFilters()) {
                    TransactionsUiState.NoSearchResults(
                        query = effectiveCriteria.query,
                        hasActiveFilters = true
                    )
                } else {
                    TransactionsUiState.Empty
                }
            } else {
                val income = filteredTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val expense = filteredTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                val groupedItems = buildGroupedList(
                    transactions = filteredTransactions,
                    categoryMap = categoryMap,
                    selectedIds = selectedIds,
                    isSelectionMode = isSelectionMode
                )
                TransactionsUiState.Success(
                    items = groupedItems,
                    totalIncome = income,
                    totalExpense = expense,
                    hasActiveFilters = effectiveCriteria.hasActiveFilters(),
                    activeFilterCount = effectiveCriteria.activeFilterCount(),
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedIds.size
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

    // --- Add / Edit Transaction Form State & One-Time Events ---
    private val _addTransactionUiState = MutableStateFlow(AddTransactionUiState())
    val addTransactionUiState: StateFlow<AddTransactionUiState> = _addTransactionUiState.asStateFlow()

    private val _transactionUiEvents = Channel<TransactionUiEvent>(Channel.BUFFERED)
    val transactionUiEvents = _transactionUiEvents.receiveAsFlow()

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

    fun prepareEditTransaction(transaction: Transaction) {
        _addTransactionUiState.update {
            it.copy(
                selectedType = transaction.type,
                selectedCategoryId = transaction.categoryId,
                editingTransactionId = transaction.id,
                editingDate = transaction.date,
                errorMessage = null,
                isSaved = false,
                isUpdated = false
            )
        }
        loadCategoriesForCurrentType()
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

        val editingId = _addTransactionUiState.value.editingTransactionId
        val editingDate = _addTransactionUiState.value.editingDate ?: System.currentTimeMillis()
        val transactionType = _addTransactionUiState.value.selectedType

        viewModelScope.launch {
            if (editingId != null) {
                val result = updateTransactionUseCase(
                    id = editingId,
                    amount = amount,
                    type = transactionType,
                    categoryId = categoryId,
                    title = title.trim(),
                    note = note?.trim()?.ifBlank { null },
                    date = editingDate
                )
                result.fold(
                    onSuccess = {
                        _addTransactionUiState.update {
                            it.copy(isLoading = false, isUpdated = true, errorMessage = null)
                        }
                        if (transactionType == TransactionType.EXPENSE) {
                            checkBudgetAlertUseCase(categoryId = categoryId, amount = amount)
                        }
                        _transactionUiEvents.send(TransactionUiEvent.Updated)
                    },
                    onFailure = { error ->
                        _addTransactionUiState.update {
                            it.copy(isLoading = false, errorMessage = error.message ?: "فشل تعديل المعاملة")
                        }
                    }
                )
            } else {
                val result = addTransactionUseCase(
                    amount = amount,
                    type = transactionType,
                    categoryId = categoryId,
                    title = title.trim(),
                    note = note?.trim()?.ifBlank { null }
                )
                result.fold(
                    onSuccess = {
                        _addTransactionUiState.update {
                            it.copy(isLoading = false, isSaved = true, errorMessage = null)
                        }
                        if (transactionType == TransactionType.EXPENSE) {
                            checkBudgetAlertUseCase(categoryId = categoryId, amount = amount)
                        }
                        _transactionUiEvents.send(TransactionUiEvent.Saved)
                    },
                    onFailure = { error ->
                        _addTransactionUiState.update {
                            it.copy(isLoading = false, errorMessage = error.message ?: "فشل حفظ المعاملة")
                        }
                    }
                )
            }
        }
    }

    fun resetAddTransactionState() {
        _addTransactionUiState.update {
            AddTransactionUiState(
                selectedType = TransactionType.EXPENSE,
                selectedCategoryId = null,
                isLoading = false,
                isSaved = false,
                isUpdated = false,
                errorMessage = null,
                editingTransactionId = null,
                editingDate = null
            )
        }
        loadCategoriesForCurrentType()
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
        }
    }

    // --- Multi-Selection Functions ---
    fun enterSelectionMode(initialSelectedId: Long? = null) {
        _isSelectionMode.value = true
        _selectedTransactionIds.value = if (initialSelectedId != null) setOf(initialSelectedId) else emptySet()
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedTransactionIds.value = emptySet()
    }

    fun toggleTransactionSelection(id: Long) {
        if (!_isSelectionMode.value) {
            enterSelectionMode(id)
            return
        }
        _selectedTransactionIds.update { current ->
            if (current.contains(id)) {
                val next = current - id
                if (next.isEmpty()) {
                    _isSelectionMode.value = false
                }
                next
            } else {
                current + id
            }
        }
    }

    fun bulkDeleteSelected(onSuccess: (deletedCount: Int) -> Unit, onError: (String) -> Unit) {
        if (_isBulkOperating.value) return
        val idsToDelete = _selectedTransactionIds.value
        if (idsToDelete.isEmpty()) {
            onError("لم يتم تحديد أي معاملات للحذف")
            return
        }

        viewModelScope.launch {
            _isBulkOperating.value = true
            val result = bulkDeleteTransactionsUseCase(idsToDelete)
            _isBulkOperating.value = false
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                exitSelectionMode()
                onSuccess(count)
            } else {
                onError(result.exceptionOrNull()?.message ?: "فشل حذف المعاملات المحددة")
            }
        }
    }

    fun bulkChangeCategorySelected(newCategoryId: Long, onSuccess: (updatedCount: Int) -> Unit, onError: (String) -> Unit) {
        if (_isBulkOperating.value) return
        val idsToUpdate = _selectedTransactionIds.value
        if (idsToUpdate.isEmpty()) {
            onError("لم يتم تحديد أي معاملات")
            return
        }

        viewModelScope.launch {
            _isBulkOperating.value = true
            val result = bulkChangeCategoryUseCase(idsToUpdate, newCategoryId)
            _isBulkOperating.value = false
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                exitSelectionMode()
                onSuccess(count)
            } else {
                onError(result.exceptionOrNull()?.message ?: "فشل تغيير تصنيف المعاملات")
            }
        }
    }

    private fun buildGroupedList(
        transactions: List<Transaction>,
        categoryMap: Map<Long, Category>,
        selectedIds: Set<Long> = emptySet(),
        isSelectionMode: Boolean = false
    ): List<TransactionListItem> {
        val items = mutableListOf<TransactionListItem>()
        val groupedByDate = transactions.groupBy { DateUtils.getDateGroupKey(it.date) }

        for ((dateLabel, dayTransactions) in groupedByDate) {
            items.add(TransactionListItem.Header(dateLabel))
            for (transaction in dayTransactions) {
                val category = categoryMap[transaction.categoryId]
                items.add(
                    TransactionListItem.Item(
                        transaction = transaction,
                        category = category,
                        isSelected = selectedIds.contains(transaction.id),
                        isSelectionMode = isSelectionMode
                    )
                )
            }
        }
        return items
    }
}

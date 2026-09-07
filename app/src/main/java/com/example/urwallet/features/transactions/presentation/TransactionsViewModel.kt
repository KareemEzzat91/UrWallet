package com.example.urwallet.features.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.usecase.AddTransactionUseCase
import com.example.urwallet.features.transactions.domain.usecase.DeleteTransactionUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetCategoriesUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    // --- Transactions List State ---
    val transactionsUiState: StateFlow<TransactionsUiState> = combine(
        getTransactionsUseCase(),
        getCategoriesUseCase()
    ) { transactions, categories ->
        if (transactions.isEmpty()) {
            TransactionsUiState.Empty
        } else {
            val categoryMap = categories.associateBy { it.id }
            val groupedItems = buildGroupedList(transactions, categoryMap)
            TransactionsUiState.Success(groupedItems)
        }
    }.catch { error ->
        emit(TransactionsUiState.Error(error.message ?: "حدث خطأ أثناء تحميل المعاملات"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState.Loading
    )

    // --- Add Transaction Form State ---
    // TODO(architecture): isSaved is a boolean state field, not a true one-time event.
    //   This works correctly in practice because resetAddTransactionState() is called
    //   immediately in the Fragment after dismiss(). For future cleanup, replace with
    //   a Channel<UiEffect> pattern (e.g. SharedFlow) to guarantee exactly-once delivery.
    private val _addTransactionUiState = MutableStateFlow(AddTransactionUiState())
    val addTransactionUiState: StateFlow<AddTransactionUiState> = _addTransactionUiState.asStateFlow()

    // Track the active categories collection to cancel before starting a new one
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
        // Cancel any previous active collection before starting a new one
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            getCategoriesUseCase(categoryType).collect { categories ->
                _addTransactionUiState.update { currentState ->
                    currentState.copy(
                        categories = categories,
                        // Auto-select first category if none selected
                        selectedCategoryId = currentState.selectedCategoryId ?: categories.firstOrNull()?.id
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

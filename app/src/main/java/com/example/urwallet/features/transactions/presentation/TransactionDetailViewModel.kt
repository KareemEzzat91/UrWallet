package com.example.urwallet.features.transactions.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.transactions.domain.usecase.DeleteTransactionUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetCategoriesUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetTransactionByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: -1L

    private val _uiState = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Loading)
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        loadTransaction(transactionId)
    }

    fun loadTransaction(id: Long) {
        if (id <= 0L) {
            _uiState.value = TransactionDetailUiState.Error("معرف المعاملة غير صالح")
            return
        }

        viewModelScope.launch {
            _uiState.value = TransactionDetailUiState.Loading
            combine(
                getTransactionByIdUseCase(id),
                getCategoriesUseCase()
            ) { transaction, categories ->
                if (transaction == null) {
                    TransactionDetailUiState.Error("لم يتم العثور على المعاملة")
                } else {
                    val category = categories.find { it.id == transaction.categoryId }
                    TransactionDetailUiState.Success(transaction, category)
                }
            }.catch { error ->
                emit(TransactionDetailUiState.Error(error.message ?: "حدث خطأ أثناء تحميل تفاصيل المعاملة"))
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteTransaction() {
        val currentState = _uiState.value
        val idToDelete = if (currentState is TransactionDetailUiState.Success) {
            currentState.transaction.id
        } else {
            transactionId
        }

        if (idToDelete <= 0L) return

        viewModelScope.launch {
            try {
                deleteTransactionUseCase(idToDelete)
                _uiState.value = TransactionDetailUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = TransactionDetailUiState.Error(e.message ?: "فشل حذف المعاملة")
            }
        }
    }
}

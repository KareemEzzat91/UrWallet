package com.example.urwallet.features.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.usecase.ConfirmFinancialEventUseCase
import com.example.urwallet.features.events.domain.usecase.DismissFinancialEventUseCase
import com.example.urwallet.features.events.domain.usecase.GetFinancialInboxUseCase
import com.example.urwallet.features.events.domain.usecase.ScanRecentSmsUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FinancialInboxUiEvent {
    data class ShowMessage(val message: String) : FinancialInboxUiEvent()
    data class ScanCompleted(val newCount: Int) : FinancialInboxUiEvent()
}

@HiltViewModel
class FinancialInboxViewModel @Inject constructor(
    private val getFinancialInboxUseCase: GetFinancialInboxUseCase,
    private val confirmFinancialEventUseCase: ConfirmFinancialEventUseCase,
    private val dismissFinancialEventUseCase: DismissFinancialEventUseCase,
    private val scanRecentSmsUseCase: ScanRecentSmsUseCase,
    private val transactionRepository: TransactionRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    val pendingEvents: StateFlow<List<FinancialEvent>> = getFinancialInboxUseCase.getEventsByStatus(InboxStatus.PENDING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val confirmedEvents: StateFlow<List<FinancialEvent>> = getFinancialInboxUseCase.getEventsByStatus(InboxStatus.CONFIRMED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dismissedEvents: StateFlow<List<FinancialEvent>> = getFinancialInboxUseCase.getEventsByStatus(InboxStatus.DISMISSED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = getFinancialInboxUseCase.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isSmsDetectionEnabled: StateFlow<Boolean> = appPreferences.isSmsDetectionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val categories: StateFlow<List<Category>> = transactionRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _uiEvents = MutableSharedFlow<FinancialInboxUiEvent>()
    val uiEvents: SharedFlow<FinancialInboxUiEvent> = _uiEvents.asSharedFlow()

    fun setSmsDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setSmsDetectionEnabled(enabled)
        }
    }

    fun scanRecentSms() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val newEventsCount = scanRecentSmsUseCase(daysBack = 14)
                _uiEvents.emit(FinancialInboxUiEvent.ScanCompleted(newEventsCount))
            } catch (e: Exception) {
                _uiEvents.emit(FinancialInboxUiEvent.ShowMessage("حدث خطأ أثناء فحص الرسائل"))
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun confirmEvent(
        eventId: Long,
        categoryId: Long,
        title: String,
        note: String? = null,
        date: Long? = null,
        counterparty: Counterparty? = null,
        saveCounterpartyMapping: Boolean = true
    ) {
        viewModelScope.launch {
            val result = confirmFinancialEventUseCase(
                eventId = eventId,
                categoryId = categoryId,
                title = title,
                note = note,
                date = date,
                counterparty = counterparty,
                saveCounterpartyMapping = saveCounterpartyMapping
            )
            result.onSuccess {
                _uiEvents.emit(FinancialInboxUiEvent.ShowMessage("تم تأكيد المعاملة وإضافتها لمحفظتك"))
            }.onFailure { err ->
                _uiEvents.emit(FinancialInboxUiEvent.ShowMessage(err.message ?: "فشل في تأكيد المعاملة"))
            }
        }
    }

    fun dismissEvent(eventId: Long) {
        viewModelScope.launch {
            dismissFinancialEventUseCase(eventId)
            _uiEvents.emit(FinancialInboxUiEvent.ShowMessage("تم تجاهل المعاملة ومسح نصها"))
        }
    }
}

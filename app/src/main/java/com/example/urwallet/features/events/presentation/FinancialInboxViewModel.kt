package com.example.urwallet.features.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.features.events.domain.model.CategorySuggestion
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxFilter
import com.example.urwallet.features.events.domain.model.InboxSortOrder
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.model.InboxTab
import com.example.urwallet.features.events.domain.model.ObligationSettlementSuggestion
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import com.example.urwallet.features.events.domain.usecase.ConfirmFinancialEventUseCase
import com.example.urwallet.features.events.domain.usecase.DismissFinancialEventUseCase
import com.example.urwallet.features.events.domain.usecase.GetFinancialInboxUseCase
import com.example.urwallet.features.events.domain.usecase.ScanRecentSmsUseCase
import com.example.urwallet.features.events.domain.usecase.SuggestCategoryUseCase
import com.example.urwallet.features.events.domain.usecase.SuggestObligationSettlementUseCase
import com.example.urwallet.features.people.domain.usecase.ResolvePersonForCounterpartyUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FinancialInboxUiEvent {
    data class ShowMessage(val message: String) : FinancialInboxUiEvent()
    data class ScanCompleted(val newCount: Int) : FinancialInboxUiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinancialInboxViewModel @Inject constructor(
    private val getFinancialInboxUseCase: GetFinancialInboxUseCase,
    private val confirmFinancialEventUseCase: ConfirmFinancialEventUseCase,
    private val dismissFinancialEventUseCase: DismissFinancialEventUseCase,
    private val scanRecentSmsUseCase: ScanRecentSmsUseCase,
    private val financialEventRepository: FinancialEventRepository,
    private val suggestCategoryUseCase: SuggestCategoryUseCase,
    private val suggestObligationSettlementUseCase: SuggestObligationSettlementUseCase,
    private val resolvePersonForCounterpartyUseCase: ResolvePersonForCounterpartyUseCase,
    private val transactionRepository: TransactionRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _filter = MutableStateFlow(InboxFilter(tab = InboxTab.PENDING))
    val filter: StateFlow<InboxFilter> = _filter.asStateFlow()

    val filteredEvents: StateFlow<List<FinancialEvent>> = _filter
        .flatMapLatest { currentFilter ->
            getFinancialInboxUseCase.getFilteredEvents(currentFilter)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingEvents: StateFlow<List<FinancialEvent>> = getFinancialInboxUseCase.getEventsByStatus(InboxStatus.PENDING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = getFinancialInboxUseCase.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isSmsDetectionEnabled: StateFlow<Boolean> = appPreferences.isSmsDetectionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val categories: StateFlow<List<Category>> = transactionRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Multi-selection state for safe bulk review actions
    private val _selectedEventIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedEventIds: StateFlow<Set<Long>> = _selectedEventIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedEventIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiEvents = MutableSharedFlow<FinancialInboxUiEvent>()
    val uiEvents: SharedFlow<FinancialInboxUiEvent> = _uiEvents.asSharedFlow()

    fun setFilterTab(tab: InboxTab) {
        _filter.value = _filter.value.copy(tab = tab)
        clearSelection()
    }

    fun setSearchQuery(query: String?) {
        _filter.value = _filter.value.copy(query = query?.trim().orEmpty())
    }

    fun setSortOrder(sortOrder: InboxSortOrder) {
        _filter.value = _filter.value.copy(sortOrder = sortOrder)
    }

    fun toggleEventSelection(eventId: Long) {
        val current = _selectedEventIds.value.toMutableSet()
        if (current.contains(eventId)) {
            current.remove(eventId)
        } else {
            current.add(eventId)
        }
        _selectedEventIds.value = current
    }

    fun selectAll() {
        val allIds = filteredEvents.value.map { it.id }.toSet()
        _selectedEventIds.value = allIds
    }

    fun clearSelection() {
        _selectedEventIds.value = emptySet()
    }

    // Safe bulk actions: Only Dismiss and Mark for Review (NO Confirm All)
    fun dismissSelected() {
        val idsToDismiss = _selectedEventIds.value.toList()
        if (idsToDismiss.isEmpty()) return

        viewModelScope.launch {
            financialEventRepository.markDismissedBulk(idsToDismiss)
            clearSelection()
            _uiEvents.emit(FinancialInboxUiEvent.ShowMessage("تم تجاهل ${idsToDismiss.size} معاملة ومسح نصوصها"))
        }
    }

    fun markSelectedForReview() {
        val idsToReview = _selectedEventIds.value.toList()
        if (idsToReview.isEmpty()) return

        viewModelScope.launch {
            financialEventRepository.markPendingBulk(idsToReview)
            clearSelection()
            _uiEvents.emit(FinancialInboxUiEvent.ShowMessage("تم وضع ${idsToReview.size} معاملة للمراجعة"))
        }
    }

    // Duplicate resolution options
    fun resolveDuplicateSameTransaction(event: FinancialEvent) {
        val matchedTxId = event.matchedTransactionId ?: return
        viewModelScope.launch {
            financialEventRepository.markConfirmed(event.id, matchedTxId)
            _uiEvents.emit(FinancialInboxUiEvent.ShowMessage("تم ربط المعاملة بالمعاملة السابقة دون تكرار"))
        }
    }

    fun resolveDuplicateDifferentTransaction(event: FinancialEvent) {
        viewModelScope.launch {
            financialEventRepository.markAsDifferentTransaction(event.id)
            _uiEvents.emit(FinancialInboxUiEvent.ShowMessage("تم تحديدها كمعاملة منفصلة، جاهزة للتأكيد"))
        }
    }

    suspend fun getExistingTransaction(transactionId: Long): Transaction? {
        return transactionRepository.getTransactionById(transactionId).firstOrNull()
    }

    suspend fun getCategorySuggestionForEvent(event: FinancialEvent): CategorySuggestion? {
        return suggestCategoryUseCase(
            merchantOrCounterparty = event.counterparty?.name ?: event.sender,
            rawMessage = event.rawMessage,
            type = event.type
        )
    }

    suspend fun getObligationSuggestionsForEvent(event: FinancialEvent): List<ObligationSettlementSuggestion> {
        val person = resolvePersonForCounterpartyUseCase(
            phoneNumber = event.counterparty?.phoneNumber,
            counterpartyName = event.counterparty?.name
        )
        if (person == null) return emptyList()

        return suggestObligationSettlementUseCase(
            personId = person.id,
            amount = event.amount,
            type = event.type
        )
    }

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
        personId: Long? = null,
        saveCounterpartyMapping: Boolean = true,
        saveCategoryMapping: Boolean = true,
        settleObligationId: Long? = null
    ) {
        viewModelScope.launch {
            val result = confirmFinancialEventUseCase(
                eventId = eventId,
                categoryId = categoryId,
                title = title,
                note = note,
                date = date,
                counterparty = counterparty,
                personId = personId,
                saveCounterpartyMapping = saveCounterpartyMapping,
                saveCategoryMapping = saveCategoryMapping,
                settleObligationId = settleObligationId
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

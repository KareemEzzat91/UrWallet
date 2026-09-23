package com.example.urwallet.features.people.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.model.PersonFinancialSummary
import com.example.urwallet.features.people.domain.usecase.CreateFinancialObligationUseCase
import com.example.urwallet.features.people.domain.usecase.DeletePersonUseCase
import com.example.urwallet.features.people.domain.usecase.GetPersonFinancialSummaryUseCase
import com.example.urwallet.features.people.domain.usecase.GetPersonObligationsUseCase
import com.example.urwallet.features.people.domain.usecase.GetPersonTransactionsUseCase
import com.example.urwallet.features.people.domain.usecase.GetPersonUseCase
import com.example.urwallet.features.people.domain.usecase.SettleObligationUseCase
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiEvent {
    data class ShowMessage(val message: String) : ProfileUiEvent()
    data class ShowError(val error: String) : ProfileUiEvent()
    object PersonDeleted : ProfileUiEvent()
}

@HiltViewModel
class PersonProfileViewModel @Inject constructor(
    private val getPersonUseCase: GetPersonUseCase,
    private val getPersonFinancialSummaryUseCase: GetPersonFinancialSummaryUseCase,
    private val getPersonObligationsUseCase: GetPersonObligationsUseCase,
    private val getPersonTransactionsUseCase: GetPersonTransactionsUseCase,
    private val createFinancialObligationUseCase: CreateFinancialObligationUseCase,
    private val settleObligationUseCase: SettleObligationUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val categoryRepository: CategoryRepository,
    private val deletePersonUseCase: DeletePersonUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val personId: Long = savedStateHandle["personId"] ?: 0L

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent: SharedFlow<ProfileUiEvent> = _uiEvent.asSharedFlow()

    val summary: StateFlow<PersonFinancialSummary?> = getPersonFinancialSummaryUseCase(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val person: StateFlow<Person?> = getPersonUseCase(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val obligations: StateFlow<List<FinancialObligation>> = getPersonObligationsUseCase(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = getPersonTransactionsUseCase(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addObligation(
        title: String,
        amount: Double,
        direction: ObligationDirection,
        dueDate: Long?,
        notes: String?
    ) {
        viewModelScope.launch {
            val result = createFinancialObligationUseCase(
                personId = personId,
                amount = amount,
                direction = direction,
                reason = title.takeIf { it.isNotBlank() } ?: notes,
                dueDate = dueDate
            )
            result.fold(
                onSuccess = {
                    _uiEvent.emit(ProfileUiEvent.ShowMessage("تم تسجيل الالتزام بنجاح"))
                },
                onFailure = { e ->
                    _uiEvent.emit(ProfileUiEvent.ShowError(e.message ?: "حدث خطأ أثناء تسجيل الالتزام"))
                }
            )
        }
    }

    fun settleObligation(
        obligationId: Long,
        amount: Double,
        note: String?,
        createTransaction: Boolean
    ) {
        viewModelScope.launch {
            val result = settleObligationUseCase(
                obligationId = obligationId,
                amount = amount,
                note = note,
                date = System.currentTimeMillis()
            )

            result.fold(
                onSuccess = { updatedOb ->
                    if (createTransaction) {
                        try {
                            val isIncome = updatedOb.direction == ObligationDirection.OWED_TO_ME
                            val targetType = if (isIncome) CategoryType.INCOME else CategoryType.EXPENSE
                            val defaultCat = categoryRepository.getCategoriesByType(targetType).firstOrNull()?.firstOrNull()
                                ?: categoryRepository.getAllCategories().firstOrNull()?.firstOrNull()
                            val catId = defaultCat?.id ?: 1L
                            addTransactionUseCase(
                                amount = amount,
                                type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                                categoryId = catId,
                                title = "سداد: ${updatedOb.reason ?: "التزام مالي"}",
                                note = note,
                                personId = personId
                            )
                        } catch (e: Exception) {
                            // Ledger insert non-fatal
                        }
                    }
                    _uiEvent.emit(ProfileUiEvent.ShowMessage("تم تسجيل السداد بنجاح"))
                },
                onFailure = { e ->
                    _uiEvent.emit(ProfileUiEvent.ShowError(e.message ?: "حدث خطأ أثناء تسجيل السداد"))
                }
            )
        }
    }

    fun deletePerson() {
        viewModelScope.launch {
            deletePersonUseCase(personId).fold(
                onSuccess = {
                    _uiEvent.emit(ProfileUiEvent.ShowMessage("تم حذف الشخص بنجاح"))
                    _uiEvent.emit(ProfileUiEvent.PersonDeleted)
                },
                onFailure = { e ->
                    _uiEvent.emit(ProfileUiEvent.ShowError(e.message ?: "حدث خطأ أثناء حذف الشخص"))
                }
            )
        }
    }
}

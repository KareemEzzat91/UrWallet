package com.example.urwallet.features.people.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.people.domain.model.PeopleAnalyticsSummary
import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.model.PersonFinancialSummary
import com.example.urwallet.features.people.domain.usecase.CreatePersonUseCase
import com.example.urwallet.features.people.domain.usecase.DeletePersonUseCase
import com.example.urwallet.features.people.domain.usecase.GetPeopleAnalyticsUseCase
import com.example.urwallet.features.people.domain.usecase.GetPeopleUseCase
import com.example.urwallet.features.people.domain.usecase.GetPersonFinancialSummaryUseCase
import com.example.urwallet.features.people.domain.usecase.UpdatePersonUseCase
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PeopleUiEvent {
    data class ShowMessage(val message: String) : PeopleUiEvent()
    data class ShowError(val error: String) : PeopleUiEvent()
    data class PersonCreated(val personId: Long) : PeopleUiEvent()
}

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val getPeopleUseCase: GetPeopleUseCase,
    private val getPeopleAnalyticsUseCase: GetPeopleAnalyticsUseCase,
    private val getPersonFinancialSummaryUseCase: GetPersonFinancialSummaryUseCase,
    private val createPersonUseCase: CreatePersonUseCase,
    private val updatePersonUseCase: UpdatePersonUseCase,
    private val deletePersonUseCase: DeletePersonUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiEvent = MutableSharedFlow<PeopleUiEvent>()
    val uiEvent: SharedFlow<PeopleUiEvent> = _uiEvent.asSharedFlow()

    val analytics: StateFlow<PeopleAnalyticsSummary?> = getPeopleAnalyticsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val peopleSummaries: StateFlow<List<PersonFinancialSummary>> = _searchQuery
        .flatMapLatest { query ->
            getPeopleUseCase(query)
        }
        .flatMapLatest { peopleList ->
            if (peopleList.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(peopleList.map { person -> getPersonFinancialSummaryUseCase(person.id) }) { array ->
                    array.filterNotNull()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createPerson(
        name: String,
        phoneNumber: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            createPersonUseCase(
                name = name,
                phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
                notes = notes?.takeIf { it.isNotBlank() }
            ).fold(
                onSuccess = { id ->
                    _uiEvent.emit(PeopleUiEvent.PersonCreated(id))
                    _uiEvent.emit(PeopleUiEvent.ShowMessage("تمت إضافة الشخص بنجاح"))
                },
                onFailure = { e ->
                    _uiEvent.emit(PeopleUiEvent.ShowError(e.message ?: "حدث خطأ أثناء إضافة الشخص"))
                }
            )
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            updatePersonUseCase(
                id = person.id,
                name = person.name,
                phoneNumber = person.phoneNumber,
                notes = person.notes
            ).fold(
                onSuccess = {
                    _uiEvent.emit(PeopleUiEvent.ShowMessage("تم تحديث بيانات الشخص بنجاح"))
                },
                onFailure = { e ->
                    _uiEvent.emit(PeopleUiEvent.ShowError(e.message ?: "حدث خطأ أثناء التحديث"))
                }
            )
        }
    }

    fun deletePerson(personId: Long) {
        viewModelScope.launch {
            deletePersonUseCase(personId).fold(
                onSuccess = {
                    _uiEvent.emit(PeopleUiEvent.ShowMessage("تم حذف الشخص بنجاح"))
                },
                onFailure = { e ->
                    _uiEvent.emit(PeopleUiEvent.ShowError(e.message ?: "حدث خطأ أثناء الحذف"))
                }
            )
        }
    }
}

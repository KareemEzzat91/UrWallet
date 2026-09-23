package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPeopleUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository
) {
    operator fun invoke(query: String = ""): Flow<List<Person>> {
        return if (query.isBlank()) {
            peopleRepository.getAllPeople()
        } else {
            peopleRepository.searchPeople(query.trim())
        }
    }
}

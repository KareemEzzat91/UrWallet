package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPersonUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository
) {
    operator fun invoke(personId: Long): Flow<Person?> {
        return peopleRepository.getPersonById(personId)
    }
}

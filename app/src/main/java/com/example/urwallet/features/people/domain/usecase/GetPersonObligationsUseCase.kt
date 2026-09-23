package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPersonObligationsUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository
) {
    operator fun invoke(personId: Long): Flow<List<FinancialObligation>> {
        return peopleRepository.getObligationsByPerson(personId)
    }
}

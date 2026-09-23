package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import javax.inject.Inject

class ResolvePersonForCounterpartyUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val financialEventRepository: FinancialEventRepository
) {
    suspend operator fun invoke(
        phoneNumber: String? = null,
        counterpartyName: String? = null
    ): Person? {
        val cleanPhone = phoneNumber?.trim()?.ifBlank { null }
        val cleanName = counterpartyName?.trim()?.ifBlank { null }

        // 1. Check counterparty phone mapping if personId is stored
        if (cleanPhone != null) {
            val mapping = financialEventRepository.getMappingForPhone(cleanPhone)
            if (mapping?.personId != null) {
                val person = peopleRepository.getPersonByIdSync(mapping.personId)
                if (person != null) return person
            }

            // 2. Direct phone lookup in people
            val personByPhone = peopleRepository.getPersonByPhone(cleanPhone)
            if (personByPhone != null) return personByPhone
        }

        // 3. Name lookup in people
        if (cleanName != null) {
            val personByName = peopleRepository.getPersonByName(cleanName)
            if (personByName != null) return personByName
        }

        return null
    }
}

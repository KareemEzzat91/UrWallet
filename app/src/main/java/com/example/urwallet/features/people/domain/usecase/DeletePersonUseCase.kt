package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.people.domain.repository.PeopleRepository
import javax.inject.Inject

class DeletePersonUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository
) {
    suspend operator fun invoke(personId: Long): Result<Unit> {
        val person = peopleRepository.getPersonByIdSync(personId)
            ?: return Result.failure(IllegalArgumentException("الشخص غير موجود"))

        // Refinement 4: Check for active unsettled obligations before deleting
        val activeObligations = peopleRepository.getActiveObligationsByPersonSync(personId)
        val hasUnsettled = activeObligations.any { it.remainingAmount > 0.001 }
        if (hasUnsettled) {
            return Result.failure(
                IllegalStateException("لا يمكن حذف ${person.name} لوجود التزامات مالية قائمة وغير مسددة. يرجى تسوية الالتزامات أولاً.")
            )
        }

        return runCatching {
            // Unlinks transactions to preserve complete financial ledger, then removes person
            peopleRepository.deletePerson(personId)
        }
    }
}

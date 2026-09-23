package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import javax.inject.Inject

class UpdatePersonUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository
) {
    suspend operator fun invoke(
        id: Long,
        name: String,
        phoneNumber: String? = null,
        notes: String? = null
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الشخص"))
        }

        val existing = peopleRepository.getPersonByIdSync(id)
            ?: return Result.failure(IllegalArgumentException("الشخص غير موجود"))

        val cleanPhone = phoneNumber?.trim()?.ifBlank { null }
        if (cleanPhone != null && cleanPhone != existing.phoneNumber) {
            val duplicatePhone = peopleRepository.getPersonByPhone(cleanPhone)
            if (duplicatePhone != null && duplicatePhone.id != id) {
                return Result.failure(IllegalStateException("رقم الهاتف $cleanPhone مسجل بالفعل لشخص آخر"))
            }
        }

        return runCatching {
            peopleRepository.updatePerson(
                existing.copy(
                    name = trimmedName,
                    phoneNumber = cleanPhone,
                    notes = notes?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import javax.inject.Inject

class CreatePersonUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository
) {
    suspend operator fun invoke(
        name: String,
        phoneNumber: String? = null,
        notes: String? = null
    ): Result<Long> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الشخص"))
        }

        // Check if person with same name or phone already exists
        val existingByName = peopleRepository.getPersonByName(trimmedName)
        if (existingByName != null) {
            return Result.failure(IllegalStateException("يوجد شخص مسجل بالفعل بهذا الاسم: $trimmedName"))
        }

        val cleanPhone = phoneNumber?.trim()?.ifBlank { null }
        if (cleanPhone != null) {
            val existingByPhone = peopleRepository.getPersonByPhone(cleanPhone)
            if (existingByPhone != null) {
                return Result.failure(IllegalStateException("رقم الهاتف $cleanPhone مسجل بالفعل للشخص: ${existingByPhone.name}"))
            }
        }

        return runCatching {
            peopleRepository.insertPerson(
                Person(
                    name = trimmedName,
                    phoneNumber = cleanPhone,
                    notes = notes?.trim()?.ifBlank { null }
                )
            )
        }
    }
}

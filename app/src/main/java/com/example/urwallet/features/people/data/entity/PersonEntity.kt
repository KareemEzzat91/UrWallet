package com.example.urwallet.features.people.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.urwallet.features.people.domain.model.Person

@Entity(
    tableName = "people",
    indices = [
        Index("name"),
        Index("phoneNumber")
    ]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Person = Person(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(person: Person): PersonEntity = PersonEntity(
            id = person.id,
            name = person.name.trim(),
            phoneNumber = person.phoneNumber?.trim()?.ifBlank { null },
            notes = person.notes?.trim()?.ifBlank { null },
            createdAt = person.createdAt,
            updatedAt = person.updatedAt
        )
    }
}

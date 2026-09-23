package com.example.urwallet.features.events.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.urwallet.features.events.domain.model.CounterpartyMapping
import com.example.urwallet.features.events.domain.model.CounterpartyType

@Entity(tableName = "counterparty_mappings")
data class CounterpartyMappingEntity(
    @PrimaryKey
    val phoneNumber: String,
    val name: String,
    val type: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CounterpartyMapping {
        return CounterpartyMapping(
            phoneNumber = phoneNumber,
            name = name,
            type = runCatching { CounterpartyType.valueOf(type) }.getOrDefault(CounterpartyType.PERSON),
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(mapping: CounterpartyMapping): CounterpartyMappingEntity {
            return CounterpartyMappingEntity(
                phoneNumber = mapping.phoneNumber,
                name = mapping.name,
                type = mapping.type.name,
                updatedAt = mapping.updatedAt
            )
        }
    }
}

package com.example.urwallet.features.events.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.urwallet.features.events.domain.model.CategoryMapping

@Entity(tableName = "category_mappings")
data class CategoryMappingEntity(
    @PrimaryKey
    val pattern: String,
    val categoryId: Long,
    val usageCount: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CategoryMapping = CategoryMapping(
        pattern = pattern,
        categoryId = categoryId,
        usageCount = usageCount,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(mapping: CategoryMapping): CategoryMappingEntity = CategoryMappingEntity(
            pattern = mapping.pattern.trim().lowercase(),
            categoryId = mapping.categoryId,
            usageCount = mapping.usageCount,
            updatedAt = mapping.updatedAt
        )
    }
}

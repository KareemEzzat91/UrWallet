package com.example.urwallet.features.events.domain.model

data class CategoryMapping(
    val pattern: String,
    val categoryId: Long,
    val usageCount: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

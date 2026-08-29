package com.example.urwallet.features.transactions.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.urwallet.core.common.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val icon: String,
    val color: String,
    val isDefault: Boolean = false,
    val isDeleted: Boolean = false
)

package com.example.urwallet.features.transactions.domain.model

import com.example.urwallet.core.common.CategoryType

data class Category(
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val icon: String,
    val color: String,
    val isDefault: Boolean = false,
    val isDeleted: Boolean = false
)

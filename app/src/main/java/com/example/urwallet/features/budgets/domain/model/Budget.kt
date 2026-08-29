package com.example.urwallet.features.budgets.domain.model

data class Budget(
    val id: Long = 0,
    val categoryId: Long? = null,
    val amount: Double,
    val month: Int,
    val year: Int,
    val alertThreshold: Double = 0.80,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isGlobal: Boolean get() = categoryId == null
}

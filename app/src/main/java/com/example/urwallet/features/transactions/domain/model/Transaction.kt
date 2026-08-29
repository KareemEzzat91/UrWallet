package com.example.urwallet.features.transactions.domain.model

import com.example.urwallet.core.common.TransactionType

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val title: String,
    val note: String? = null,
    val date: Long,
    val receiptPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

package com.example.urwallet.features.more.domain.model

import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType

data class RecurringTransaction(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val frequency: Frequency,
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrence: Long,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

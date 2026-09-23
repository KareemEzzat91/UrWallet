package com.example.urwallet.features.transactions.data.entity

import com.example.urwallet.core.common.TransactionType

data class MonthlyCashFlowEntity(
    val year: Int,
    val month: Int,
    val type: TransactionType,
    val totalAmount: Double
)

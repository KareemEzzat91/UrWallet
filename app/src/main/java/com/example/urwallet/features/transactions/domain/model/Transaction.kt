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
    /**
     * Counterparty semantics: always represents the other party involved in the transaction.
     * When type == EXPENSE: personId is the recipient (money sent TO this person).
     * When type == INCOME: personId is the payer (money received FROM this person).
     */
    val personId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

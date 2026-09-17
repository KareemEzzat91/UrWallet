package com.example.urwallet.features.transactions.presentation

/**
 * One-time UI events for transaction creation and modification.
 * Delivered via Channel to guarantee exactly-once consumption without state pollution.
 */
sealed class TransactionUiEvent {
    object Saved : TransactionUiEvent()
    object Updated : TransactionUiEvent()
}

package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(id: Long) {
        transactionRepository.deleteTransactionById(id)
    }
}

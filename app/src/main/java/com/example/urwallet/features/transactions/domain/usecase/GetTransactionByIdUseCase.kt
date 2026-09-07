package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionByIdUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(id: Long): Flow<Transaction?> {
        return transactionRepository.getTransactionById(id)
    }
}

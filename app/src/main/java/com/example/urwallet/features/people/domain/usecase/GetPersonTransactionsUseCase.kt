package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPersonTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(personId: Long): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsByPerson(personId)
    }
}

package com.example.urwallet.features.more.domain.usecase

import com.example.urwallet.features.more.domain.repository.RecurringRepository
import javax.inject.Inject

class DeleteRecurringTransactionUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository
) {
    suspend operator fun invoke(id: Long) {
        recurringRepository.deleteRecurringTransaction(id)
    }
}

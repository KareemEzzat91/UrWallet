package com.example.urwallet.features.more.domain.usecase

import com.example.urwallet.features.more.domain.repository.RecurringRepository
import javax.inject.Inject

class ToggleRecurringTransactionUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository
) {
    suspend operator fun invoke(id: Long, isActive: Boolean) {
        recurringRepository.toggleActive(id, isActive)
    }
}

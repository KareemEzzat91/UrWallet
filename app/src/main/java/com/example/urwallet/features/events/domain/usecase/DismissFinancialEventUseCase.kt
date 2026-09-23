package com.example.urwallet.features.events.domain.usecase

import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import javax.inject.Inject

class DismissFinancialEventUseCase @Inject constructor(
    private val financialEventRepository: FinancialEventRepository
) {
    suspend operator fun invoke(eventId: Long) {
        financialEventRepository.markDismissed(eventId)
    }
}

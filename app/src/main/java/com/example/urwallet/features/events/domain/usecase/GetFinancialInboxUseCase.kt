package com.example.urwallet.features.events.domain.usecase

import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFinancialInboxUseCase @Inject constructor(
    private val financialEventRepository: FinancialEventRepository
) {
    fun getPendingEvents(): Flow<List<FinancialEvent>> {
        return financialEventRepository.getPendingEvents()
    }

    fun getAllEvents(): Flow<List<FinancialEvent>> {
        return financialEventRepository.getAllEvents()
    }

    fun getEventsByStatus(status: InboxStatus): Flow<List<FinancialEvent>> {
        return financialEventRepository.getEventsByStatus(status)
    }

    fun getPendingCount(): Flow<Int> {
        return financialEventRepository.getPendingCount()
    }
}

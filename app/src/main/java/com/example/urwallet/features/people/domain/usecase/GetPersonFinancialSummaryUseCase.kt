package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationStatus
import com.example.urwallet.features.people.domain.model.PersonFinancialSummary
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetPersonFinancialSummaryUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(personId: Long): Flow<PersonFinancialSummary?> {
        return combine(
            peopleRepository.getPersonById(personId),
            transactionRepository.getTransactionsByPerson(personId),
            peopleRepository.getObligationsByPerson(personId)
        ) { person, transactions, obligations ->
            if (person == null) return@combine null

            var totalSent = 0.0
            var totalReceived = 0.0
            var lastDate: Long? = null

            for (tx in transactions) {
                if (lastDate == null || tx.date > lastDate) {
                    lastDate = tx.date
                }
                when (tx.type) {
                    TransactionType.EXPENSE -> totalSent += tx.amount
                    TransactionType.INCOME -> totalReceived += tx.amount
                }
            }

            var totalOwedToMe = 0.0
            var totalIOwe = 0.0
            var openCount = 0

            for (ob in obligations) {
                if (ob.status != ObligationStatus.SETTLED && ob.remainingAmount > 0.001) {
                    openCount++
                    when (ob.direction) {
                        ObligationDirection.OWED_TO_ME -> totalOwedToMe += ob.remainingAmount
                        ObligationDirection.I_OWE -> totalIOwe += ob.remainingAmount
                    }
                }
            }

            PersonFinancialSummary(
                person = person,
                totalSent = totalSent,
                totalReceived = totalReceived,
                transactionCount = transactions.size,
                lastTransactionDate = lastDate,
                totalOwedToMe = totalOwedToMe,
                totalIOwe = totalIOwe,
                openObligationsCount = openCount
            )
        }
    }
}

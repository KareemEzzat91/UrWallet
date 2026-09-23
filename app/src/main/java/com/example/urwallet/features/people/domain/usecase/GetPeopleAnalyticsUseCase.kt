package com.example.urwallet.features.people.domain.usecase

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationStatus
import com.example.urwallet.features.people.domain.model.PeopleAnalyticsSummary
import com.example.urwallet.features.people.domain.model.PersonFinancialSummary
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetPeopleAnalyticsUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<PeopleAnalyticsSummary> {
        return combine(
            peopleRepository.getAllPeople(),
            transactionRepository.getAllTransactions(),
            peopleRepository.getAllObligations()
        ) { people, transactions, obligations ->
            if (people.isEmpty()) {
                return@combine PeopleAnalyticsSummary()
            }

            var totalOwedToMe = 0.0
            var totalIOwe = 0.0
            var largestDebtOwedToMe: FinancialObligation? = null
            var largestDebtIOwe: FinancialObligation? = null

            for (ob in obligations) {
                if (ob.status != ObligationStatus.SETTLED && ob.remainingAmount > 0.001) {
                    when (ob.direction) {
                        ObligationDirection.OWED_TO_ME -> {
                            totalOwedToMe += ob.remainingAmount
                            if (largestDebtOwedToMe == null || ob.remainingAmount > largestDebtOwedToMe!!.remainingAmount) {
                                largestDebtOwedToMe = ob
                            }
                        }
                        ObligationDirection.I_OWE -> {
                            totalIOwe += ob.remainingAmount
                            if (largestDebtIOwe == null || ob.remainingAmount > largestDebtIOwe!!.remainingAmount) {
                                largestDebtIOwe = ob
                            }
                        }
                    }
                }
            }

            // Group transactions by personId
            val txByPerson = transactions.filter { it.personId != null }.groupBy { it.personId!! }
            val obByPerson = obligations.groupBy { it.personId }

            val summaries = people.map { person ->
                val pTx = txByPerson[person.id].orEmpty()
                val pOb = obByPerson[person.id].orEmpty()

                var sent = 0.0
                var received = 0.0
                var lastDate: Long? = null

                for (tx in pTx) {
                    if (lastDate == null || tx.date > lastDate) lastDate = tx.date
                    when (tx.type) {
                        TransactionType.EXPENSE -> sent += tx.amount
                        TransactionType.INCOME -> received += tx.amount
                    }
                }

                var owedToMe = 0.0
                var iOwe = 0.0
                var openCount = 0

                for (ob in pOb) {
                    if (ob.status != ObligationStatus.SETTLED && ob.remainingAmount > 0.001) {
                        openCount++
                        when (ob.direction) {
                            ObligationDirection.OWED_TO_ME -> owedToMe += ob.remainingAmount
                            ObligationDirection.I_OWE -> iOwe += ob.remainingAmount
                        }
                    }
                }

                PersonFinancialSummary(
                    person = person,
                    totalSent = sent,
                    totalReceived = received,
                    transactionCount = pTx.size,
                    lastTransactionDate = lastDate,
                    totalOwedToMe = owedToMe,
                    totalIOwe = iOwe,
                    openObligationsCount = openCount
                )
            }

            val mostSent = summaries.filter { it.totalSent > 0.0 }.maxByOrNull { it.totalSent }
            val mostReceived = summaries.filter { it.totalReceived > 0.0 }.maxByOrNull { it.totalReceived }
            val mostActive = summaries.filter { it.transactionCount > 0 }.maxByOrNull { it.transactionCount }
            val activeRelationships = summaries.count { it.transactionCount > 0 || it.hasActiveObligations }

            PeopleAnalyticsSummary(
                totalPeopleCount = people.size,
                activeRelationshipsCount = activeRelationships,
                totalOwedToMe = totalOwedToMe,
                totalIOwe = totalIOwe,
                mostMoneySentTo = mostSent,
                mostMoneyReceivedFrom = mostReceived,
                mostActivePerson = mostActive,
                largestDebtOwedToMe = largestDebtOwedToMe,
                largestDebtIOwe = largestDebtIOwe
            )
        }
    }
}

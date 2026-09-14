package com.example.urwallet.features.more.domain.usecase

import com.example.urwallet.features.more.domain.calculator.NextOccurrenceCalculator
import com.example.urwallet.features.more.domain.model.RecurringSummary
import com.example.urwallet.features.more.domain.model.RecurringTransactionWithCategory
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetRecurringTransactionsUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository
) {

    operator fun invoke(): Flow<RecurringSummary> {
        return combine(
            recurringRepository.getAllRecurringTransactions(),
            transactionRepository.getAllCategories()
        ) { recurringList, categories ->
            val categoryMap = categories.associateBy { it.id }

            val items = recurringList.map { recurring ->
                val category = categoryMap[recurring.categoryId]
                RecurringTransactionWithCategory(
                    recurring = recurring,
                    categoryName = category?.name ?: "أخرى",
                    categoryIcon = category?.icon ?: "ic_other",
                    categoryColor = category?.color ?: "#78909C"
                )
            }

            val monthlyObligations = NextOccurrenceCalculator.calculateMonthlyObligations(recurringList)
            val monthlyRecurringIncome = NextOccurrenceCalculator.calculateMonthlyRecurringIncome(recurringList)
            val activeCount = recurringList.count { it.isActive }

            RecurringSummary(
                monthlyObligations = monthlyObligations,
                monthlyRecurringIncome = monthlyRecurringIncome,
                activeSubscriptionsCount = activeCount,
                items = items
            )
        }
    }
}

package com.example.urwallet.features.more.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetRecurringTransactionsUseCaseTest {

    private class FakeRecurringRepo(val items: List<RecurringTransaction>) : RecurringRepository {
        override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(items)
        override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> =
            flowOf(items.filter { it.isActive })
        override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long = 1L
        override suspend fun updateRecurringTransaction(recurring: RecurringTransaction) {}
        override suspend fun deleteRecurringTransaction(id: Long) {}
        override suspend fun toggleActive(id: Long, isActive: Boolean) {}
        override fun getRecurringTransactionById(id: Long): Flow<RecurringTransaction?> = flowOf(null)
        override suspend fun getDueRecurringTransactionsSync(currentDate: Long): List<RecurringTransaction> = emptyList()
        override suspend fun updateNextOccurrence(id: Long, nextOccurrence: Long) {}
    }

    private class FakeTxRepo(val cats: List<Category>) : TransactionRepository {
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(null)
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTotalSumByType(type: TransactionType): Flow<Double> = flowOf(0.0)
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(0)
        override suspend fun insertTransaction(transaction: Transaction): Long = 1L
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override suspend fun countTransactionsByNoteTag(tagPattern: String): Int = 0

        override fun getAllCategories(): Flow<List<Category>> = flowOf(cats)
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: Long): Category? = cats.firstOrNull { it.id == id }
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    @Test
    fun `combines categories and calculates obligations and income correctly`() = runTest {
        val categories = listOf(
            Category(id = 1, name = "فواتير", type = CategoryType.EXPENSE, icon = "ic_bills", color = "#FFA726"),
            Category(id = 2, name = "راتب", type = CategoryType.INCOME, icon = "ic_salary", color = "#00732C")
        )

        val recurringList = listOf(
            RecurringTransaction(
                id = 1,
                title = "فاتورة الكهرباء",
                amount = 200.0,
                type = TransactionType.EXPENSE,
                categoryId = 1,
                frequency = Frequency.MONTHLY,
                startDate = 0L,
                nextOccurrence = 0L,
                isActive = true
            ),
            RecurringTransaction(
                id = 2,
                title = "راتب وظيفي",
                amount = 8000.0,
                type = TransactionType.INCOME,
                categoryId = 2,
                frequency = Frequency.MONTHLY,
                startDate = 0L,
                nextOccurrence = 0L,
                isActive = true
            )
        )

        val useCase = GetRecurringTransactionsUseCase(FakeRecurringRepo(recurringList), FakeTxRepo(categories))
        val summary = useCase().first()

        assertEquals(2, summary.items.size)
        assertEquals("فواتير", summary.items[0].categoryName)
        assertEquals("راتب", summary.items[1].categoryName)
        assertEquals(200.0, summary.monthlyObligations, 0.001)
        assertEquals(8000.0, summary.monthlyRecurringIncome, 0.001)
        assertEquals(2, summary.activeSubscriptionsCount)
    }
}

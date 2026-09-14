package com.example.urwallet.features.more.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ProcessDueRecurringTransactionsUseCaseTest {

    private class FakeRecurringRepository : RecurringRepository {
        val list = mutableListOf<RecurringTransaction>()

        override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(list)
        override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> =
            flowOf(list.filter { it.isActive })

        override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long {
            val id = (list.size + 1).toLong()
            list.add(recurring.copy(id = id))
            return id
        }

        override suspend fun updateRecurringTransaction(recurring: RecurringTransaction) {
            val index = list.indexOfFirst { it.id == recurring.id }
            if (index != -1) list[index] = recurring
        }

        override suspend fun deleteRecurringTransaction(id: Long) {
            list.removeAll { it.id == id }
        }

        override suspend fun toggleActive(id: Long, isActive: Boolean) {
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) list[index] = list[index].copy(isActive = isActive)
        }

        override fun getRecurringTransactionById(id: Long): Flow<RecurringTransaction?> =
            flowOf(list.firstOrNull { it.id == id })

        override suspend fun getDueRecurringTransactionsSync(currentDate: Long): List<RecurringTransaction> {
            return list.filter { it.isActive && it.nextOccurrence <= currentDate }
        }

        override suspend fun updateNextOccurrence(id: Long, nextOccurrence: Long) {
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) list[index] = list[index].copy(nextOccurrence = nextOccurrence)
        }
    }

    private class FakeTransactionRepository : TransactionRepository {
        val insertedTransactions = mutableListOf<Transaction>()

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(insertedTransactions)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(insertedTransactions.firstOrNull { it.id == id })
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(insertedTransactions.take(limit))
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(insertedTransactions.filter { it.date in startDate..endDate })
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(insertedTransactions.filter { it.categoryId == categoryId && it.date in startDate..endDate })
        override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> =
            flowOf(insertedTransactions.filter { it.type == type && it.date in startDate..endDate }.sumOf { it.amount })
        override fun getTotalSumByType(type: TransactionType): Flow<Double> =
            flowOf(insertedTransactions.filter { it.type == type }.sumOf { it.amount })
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> =
            flowOf(insertedTransactions.filter { it.categoryId == categoryId && it.date in startDate..endDate }.sumOf { it.amount })
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> =
            flowOf(insertedTransactions.count { it.date in startOfDay..endOfDay })

        override suspend fun insertTransaction(transaction: Transaction): Long {
            val id = (insertedTransactions.size + 1).toLong()
            insertedTransactions.add(transaction.copy(id = id))
            return id
        }

        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}

        override suspend fun countTransactionsByNoteTag(tagPattern: String): Int {
            val cleanTag = tagPattern.replace("%", "")
            return insertedTransactions.count { it.note?.contains(cleanTag) == true }
        }

        override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByType(type: com.example.urwallet.core.common.CategoryType): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: Long): Category? = null
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    @Test
    fun `due recurring transaction generates transaction and advances nextOccurrence`() = runTest {
        val recurringRepo = FakeRecurringRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = ProcessDueRecurringTransactionsUseCase(recurringRepo, txRepo)

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialStart = cal.timeInMillis

        recurringRepo.list.add(
            RecurringTransaction(
                id = 1,
                title = "فاتورة الكهرباء",
                amount = 250.0,
                type = TransactionType.EXPENSE,
                categoryId = 4,
                frequency = Frequency.MONTHLY,
                startDate = initialStart,
                nextOccurrence = initialStart,
                isActive = true
            )
        )

        // Process with currentTime = Sept 14 (due date Sept 10 is in the past)
        val testNow = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 12, 0, 0)
        }.timeInMillis

        val generatedCount = useCase(testNow)

        assertEquals(1, generatedCount)
        assertEquals(1, txRepo.insertedTransactions.size)
        val createdTx = txRepo.insertedTransactions.first()
        assertEquals("فاتورة الكهرباء", createdTx.title)
        assertEquals(250.0, createdTx.amount, 0.001)

        // nextOccurrence should have advanced to October 10
        val updatedRecurring = recurringRepo.list.first()
        val nextCal = Calendar.getInstance().apply { timeInMillis = updatedRecurring.nextOccurrence }
        assertEquals(Calendar.OCTOBER, nextCal.get(Calendar.MONTH))
        assertEquals(10, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `idempotency check prevents duplicate insertion when executed twice`() = runTest {
        val recurringRepo = FakeRecurringRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = ProcessDueRecurringTransactionsUseCase(recurringRepo, txRepo)

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialStart = cal.timeInMillis

        recurringRepo.list.add(
            RecurringTransaction(
                id = 1,
                title = "فاتورة الإنترنت",
                amount = 350.0,
                type = TransactionType.EXPENSE,
                categoryId = 4,
                frequency = Frequency.MONTHLY,
                startDate = initialStart,
                nextOccurrence = initialStart,
                isActive = true
            )
        )

        val testNow = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 12, 0, 0)
        }.timeInMillis

        // First execution
        val count1 = useCase(testNow)
        assertEquals(1, count1)
        assertEquals(1, txRepo.insertedTransactions.size)

        // Simulate a retry before nextOccurrence was saved, or repeating same run:
        // Set nextOccurrence back to initialStart to simulate a worker retry scenario
        recurringRepo.list[0] = recurringRepo.list[0].copy(nextOccurrence = initialStart)

        // Second execution
        val count2 = useCase(testNow)
        // Since transaction with [REC:#1@initialStart] already exists in txRepo, it must NOT insert a duplicate!
        assertEquals(0, count2)
        assertEquals(1, txRepo.insertedTransactions.size)
    }

    @Test
    fun `reaching end date marks recurring transaction as inactive`() = runTest {
        val recurringRepo = FakeRecurringRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = ProcessDueRecurringTransactionsUseCase(recurringRepo, txRepo)

        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 1, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 5, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        recurringRepo.list.add(
            RecurringTransaction(
                id = 1,
                title = "قسط منتهي",
                amount = 100.0,
                type = TransactionType.EXPENSE,
                categoryId = 1,
                frequency = Frequency.WEEKLY, // next will be Sept 8, which is > Sept 5
                startDate = startCal.timeInMillis,
                endDate = endCal.timeInMillis,
                nextOccurrence = startCal.timeInMillis,
                isActive = true
            )
        )

        val testNow = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 2, 12, 0, 0)
        }.timeInMillis

        useCase(testNow)

        val updated = recurringRepo.list.first()
        // Next occurrence is Sept 8 > Sept 5, so it should be deactivated!
        assertFalse(updated.isActive)
    }
}

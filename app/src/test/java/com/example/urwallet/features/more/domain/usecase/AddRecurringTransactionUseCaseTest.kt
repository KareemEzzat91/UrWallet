package com.example.urwallet.features.more.domain.usecase

import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddRecurringTransactionUseCaseTest {

    private class FakeRecurringRepo : RecurringRepository {
        val list = mutableListOf<RecurringTransaction>()

        override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(list)
        override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(list)
        override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long {
            val id = (list.size + 1).toLong()
            list.add(recurring.copy(id = id))
            return id
        }
        override suspend fun updateRecurringTransaction(recurring: RecurringTransaction) {}
        override suspend fun deleteRecurringTransaction(id: Long) {}
        override suspend fun toggleActive(id: Long, isActive: Boolean) {}
        override fun getRecurringTransactionById(id: Long): Flow<RecurringTransaction?> = flowOf(null)
        override suspend fun getDueRecurringTransactionsSync(currentDate: Long): List<RecurringTransaction> = emptyList()
        override suspend fun updateNextOccurrence(id: Long, nextOccurrence: Long) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank title throws IllegalArgumentException`() = runTest {
        val repo = FakeRecurringRepo()
        val useCase = AddRecurringTransactionUseCase(repo)
        useCase(
            title = "   ",
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = 1,
            frequency = Frequency.MONTHLY
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero or negative amount throws IllegalArgumentException`() = runTest {
        val repo = FakeRecurringRepo()
        val useCase = AddRecurringTransactionUseCase(repo)
        useCase(
            title = "Gym",
            amount = 0.0,
            type = TransactionType.EXPENSE,
            categoryId = 1,
            frequency = Frequency.MONTHLY
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `endDate earlier than startDate throws IllegalArgumentException`() = runTest {
        val repo = FakeRecurringRepo()
        val useCase = AddRecurringTransactionUseCase(repo)
        useCase(
            title = "Gym",
            amount = 500.0,
            type = TransactionType.EXPENSE,
            categoryId = 1,
            frequency = Frequency.MONTHLY,
            startDate = 1000000L,
            endDate = 500000L
        )
    }

    @Test
    fun `valid inputs insert transaction and return generated id`() = runTest {
        val repo = FakeRecurringRepo()
        val useCase = AddRecurringTransactionUseCase(repo)
        val id = useCase(
            title = "اشتراك نت",
            amount = 350.0,
            type = TransactionType.EXPENSE,
            categoryId = 4,
            frequency = Frequency.MONTHLY,
            startDate = 1000000L
        )

        assertEquals(1L, id)
        assertEquals(1, repo.list.size)
        assertEquals("اشتراك نت", repo.list.first().title)
    }
}

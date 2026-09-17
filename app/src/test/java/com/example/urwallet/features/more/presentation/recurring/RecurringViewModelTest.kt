package com.example.urwallet.features.more.presentation.recurring

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.more.domain.usecase.AddRecurringTransactionUseCase
import com.example.urwallet.features.more.domain.usecase.DeleteRecurringTransactionUseCase
import com.example.urwallet.features.more.domain.usecase.GetRecurringTransactionsUseCase
import com.example.urwallet.features.more.domain.usecase.ProcessDueRecurringTransactionsUseCase
import com.example.urwallet.features.more.domain.usecase.ToggleRecurringTransactionUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import com.example.urwallet.features.transactions.domain.usecase.GetCategoriesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeRecurringRepo : RecurringRepository {
        val list = mutableListOf<RecurringTransaction>()

        override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(list)
        override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(list.filter { it.isActive })
        override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long {
            val id = (list.size + 1).toLong()
            list.add(recurring.copy(id = id))
            return id
        }
        override suspend fun updateRecurringTransaction(recurring: RecurringTransaction) {}
        override suspend fun deleteRecurringTransaction(id: Long) {
            list.removeAll { it.id == id }
        }
        override suspend fun toggleActive(id: Long, isActive: Boolean) {
            val idx = list.indexOfFirst { it.id == id }
            if (idx != -1) list[idx] = list[idx].copy(isActive = isActive)
        }
        override fun getRecurringTransactionById(id: Long): Flow<RecurringTransaction?> = flowOf(list.firstOrNull { it.id == id })
        override suspend fun getDueRecurringTransactionsSync(currentDate: Long): List<RecurringTransaction> =
            list.filter { it.isActive && it.nextOccurrence <= currentDate }
        override suspend fun updateNextOccurrence(id: Long, nextOccurrence: Long) {}
    }

    private class FakeTxRepo : TransactionRepository {
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
        override fun getAllCategories(): Flow<List<Category>> = flowOf(
            listOf(Category(id = 1, name = "فواتير", type = CategoryType.EXPENSE, icon = "ic_bills", color = "#FFA726"))
        )
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: Long): Category? = null
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    private lateinit var recurringRepo: FakeRecurringRepo
    private lateinit var txRepo: FakeTxRepo
    private lateinit var viewModel: RecurringViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        recurringRepo = FakeRecurringRepo()
        txRepo = FakeTxRepo()

        recurringRepo.list.add(
            RecurringTransaction(
                id = 1,
                title = "فاتورة الإنترنت",
                amount = 350.0,
                type = TransactionType.EXPENSE,
                categoryId = 1,
                frequency = Frequency.MONTHLY,
                startDate = 1000L,
                nextOccurrence = 1000L,
                isActive = true
            )
        )

        recurringRepo.list.add(
            RecurringTransaction(
                id = 2,
                title = "راتب شهري",
                amount = 10000.0,
                type = TransactionType.INCOME,
                categoryId = 1,
                frequency = Frequency.MONTHLY,
                startDate = 1000L,
                nextOccurrence = 1000L,
                isActive = true
            )
        )

        val getRecurringUseCase = GetRecurringTransactionsUseCase(recurringRepo, txRepo)
        val addRecurringUseCase = AddRecurringTransactionUseCase(recurringRepo)
        val deleteRecurringUseCase = DeleteRecurringTransactionUseCase(recurringRepo)
        val toggleRecurringUseCase = ToggleRecurringTransactionUseCase(recurringRepo)
        val processDueUseCase = ProcessDueRecurringTransactionsUseCase(recurringRepo)
        val getCategoriesUseCase = GetCategoriesUseCase(txRepo)

        viewModel = RecurringViewModel(
            getRecurringTransactionsUseCase = getRecurringUseCase,
            addRecurringTransactionUseCase = addRecurringUseCase,
            deleteRecurringTransactionUseCase = deleteRecurringUseCase,
            toggleRecurringTransactionUseCase = toggleRecurringUseCase,
            processDueRecurringTransactionsUseCase = processDueUseCase,
            getCategoriesUseCase = getCategoriesUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState contains all items and calculated obligations`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.items.size)
        assertEquals(2, state.filteredItems.size)
        assertEquals(350.0, state.monthlyObligations, 0.001)
        assertEquals(10000.0, state.monthlyRecurringIncome, 0.001)
        assertEquals(2, state.activeSubscriptionsCount)
    }

    @Test
    fun `filter items by EXPENSE shows only expense items`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.setFilter(RecurringFilterType.EXPENSE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredItems.size)
        assertEquals("فاتورة الإنترنت", state.filteredItems.first().recurring.title)
    }

    @Test
    fun `filter items by INCOME shows only income items`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.setFilter(RecurringFilterType.INCOME)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredItems.size)
        assertEquals("راتب شهري", state.filteredItems.first().recurring.title)
    }

    @Test
    fun `toggle active updates item state`() = runTest {
        viewModel.toggleActive(1, false)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = recurringRepo.list.first { it.id == 1L }
        assertEquals(false, updated.isActive)
    }

    @Test
    fun `delete recurring removes item`() = runTest {
        viewModel.deleteRecurring(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, recurringRepo.list.size)
        assertEquals(2L, recurringRepo.list.first().id)
    }
}

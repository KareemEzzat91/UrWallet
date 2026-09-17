package com.example.urwallet.features.more.presentation.categories

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.usecase.AddCategoryUseCase
import com.example.urwallet.features.transactions.domain.usecase.DeleteCategoryUseCase
import com.example.urwallet.features.transactions.domain.usecase.GetCategoriesUseCase
import com.example.urwallet.features.transactions.domain.usecase.UpdateCategoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeCategoryRepository
    private lateinit var viewModel: CategoriesViewModel

    private class FakeCategoryRepository : CategoryRepository {
        val categories = mutableMapOf<Long, Category>()
        private val flow = MutableStateFlow<List<Category>>(emptyList())
        private var idCounter = 1L

        fun notifyUpdate() {
            flow.value = categories.values.toList()
        }

        override fun getAllCategories(): Flow<List<Category>> =
            flow.map { list -> list.filter { !it.isDeleted } }

        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
            flow.map { list -> list.filter { !it.isDeleted && (it.type == type || it.type == CategoryType.BOTH) } }

        override suspend fun getCategoryById(id: Long): Category? =
            categories[id]?.takeIf { !it.isDeleted }

        override suspend fun insertCategory(category: Category): Long {
            val assignedId = if (category.id > 0) category.id else ((categories.keys.maxOrNull() ?: 0L) + 1L)
            categories[assignedId] = category.copy(id = assignedId)
            notifyUpdate()
            return assignedId
        }

        override suspend fun updateCategory(category: Category) {
            categories[category.id] = category
            notifyUpdate()
        }

        override suspend fun deleteCategory(id: Long) {
            val existing = categories[id]
            if (existing != null) {
                categories[id] = existing.copy(isDeleted = true)
                notifyUpdate()
            }
        }
    }

    private class FakeTransactionRepoForCategories(private val repo: FakeCategoryRepository) :
        com.example.urwallet.features.transactions.domain.repository.TransactionRepository {
        override fun getAllTransactions(): Flow<List<com.example.urwallet.features.transactions.domain.model.Transaction>> = flowOf(emptyList())
        override fun getTransactionById(id: Long): Flow<com.example.urwallet.features.transactions.domain.model.Transaction?> = flowOf(null)
        override fun getRecentTransactions(limit: Int): Flow<List<com.example.urwallet.features.transactions.domain.model.Transaction>> = flowOf(emptyList())
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<com.example.urwallet.features.transactions.domain.model.Transaction>> = flowOf(emptyList())
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<com.example.urwallet.features.transactions.domain.model.Transaction>> = flowOf(emptyList())
        override fun getSumByTypeAndPeriod(type: com.example.urwallet.core.common.TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTotalSumByType(type: com.example.urwallet.core.common.TransactionType): Flow<Double> = flowOf(0.0)
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(0)
        override suspend fun insertTransaction(transaction: com.example.urwallet.features.transactions.domain.model.Transaction): Long = 1L
        override suspend fun updateTransaction(transaction: com.example.urwallet.features.transactions.domain.model.Transaction) {}
        override suspend fun deleteTransaction(transaction: com.example.urwallet.features.transactions.domain.model.Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override suspend fun countTransactionsByNoteTag(tagPattern: String): Int = 0

        override fun getAllCategories(): Flow<List<Category>> = repo.getAllCategories()
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = repo.getCategoriesByType(type)
        override suspend fun getCategoryById(id: Long): Category? = repo.getCategoryById(id)
        override suspend fun insertCategory(category: Category): Long = repo.insertCategory(category)
        override suspend fun updateCategory(category: Category) = repo.updateCategory(category)
        override suspend fun deleteCategory(id: Long) = repo.deleteCategory(id)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeCategoryRepository()

        // Seed initial categories
        fakeRepository.categories[1L] = Category(
            id = 1L,
            name = "أكل ومشروبات",
            type = CategoryType.EXPENSE,
            icon = "ic_food",
            color = "#FF7043",
            isDefault = true
        )
        fakeRepository.categories[2L] = Category(
            id = 2L,
            name = "راتب",
            type = CategoryType.INCOME,
            icon = "ic_salary",
            color = "#00732C",
            isDefault = true
        )
        fakeRepository.notifyUpdate()

        val txRepo = FakeTransactionRepoForCategories(fakeRepository)
        val getCategoriesUseCase = GetCategoriesUseCase(txRepo)
        val addCategoryUseCase = AddCategoryUseCase(fakeRepository)
        val updateCategoryUseCase = UpdateCategoryUseCase(fakeRepository)
        val deleteCategoryUseCase = DeleteCategoryUseCase(fakeRepository)

        viewModel = CategoriesViewModel(
            getCategoriesUseCase = getCategoriesUseCase,
            addCategoryUseCase = addCategoryUseCase,
            updateCategoryUseCase = updateCategoryUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads expense categories by default`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(CategoryType.EXPENSE, state.selectedType)
        assertEquals(1, state.categories.size)
        assertEquals("أكل ومشروبات", state.categories.first().name)
    }

    @Test
    fun `setTypeFilter changes filter and loads corresponding categories`() = runTest {
        viewModel.setTypeFilter(CategoryType.INCOME)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CategoryType.INCOME, state.selectedType)
        assertEquals(1, state.categories.size)
        assertEquals("راتب", state.categories.first().name)
    }

    @Test
    fun `saveCategory with id 0 adds category and emits userMessage`() = runTest {
        viewModel.saveCategory(
            id = 0L,
            name = "تسوق خاص",
            type = CategoryType.EXPENSE,
            icon = "ic_shopping",
            color = "#AB47BC"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.userMessage)
        assertEquals(2, state.categories.size)
    }

    @Test
    fun `deleteCategory on custom category removes it and emits success message`() = runTest {
        // Add custom category first
        viewModel.saveCategory(0L, "مؤقت", CategoryType.EXPENSE, "ic_other", "#78909C")
        testDispatcher.scheduler.advanceUntilIdle()

        val customCategory = viewModel.uiState.value.categories.first { it.name == "مؤقت" }
        viewModel.deleteCategory(customCategory)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.categories.size) // Only default left
        assertNotNull(state.userMessage)
    }

    @Test
    fun `deleteCategory on default category emits errorMessage`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val defaultCategory = viewModel.uiState.value.categories.first { it.isDefault }

        viewModel.deleteCategory(defaultCategory)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertEquals(1, state.categories.size) // Not deleted
    }
}

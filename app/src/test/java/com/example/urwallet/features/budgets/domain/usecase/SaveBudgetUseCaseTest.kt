package com.example.urwallet.features.budgets.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveBudgetUseCaseTest {

    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var useCase: SaveBudgetUseCase

    private class FakeBudgetRepository : BudgetRepository {
        var lastInsertedBudget: Budget? = null
        override fun getGlobalBudget(month: Int, year: Int): Flow<Budget?> = flowOf(null)
        override fun getCategoryBudgets(month: Int, year: Int): Flow<List<Budget>> = flowOf(emptyList())
        override fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<Budget?> = flowOf(null)
        override suspend fun getGlobalBudgetSync(month: Int, year: Int): Budget? = null
        override suspend fun getBudgetForCategorySync(categoryId: Long, month: Int, year: Int): Budget? = null
        override suspend fun insertOrUpdateBudget(budget: Budget): Long {
            lastInsertedBudget = budget
            return budget.id.takeIf { it > 0 } ?: 100L
        }
        override suspend fun deleteBudget(id: Long) {}
    }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = mutableMapOf<Long, Category>()

        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories.values.toList())
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
            flowOf(categories.values.filter { it.type == type })
        override suspend fun getCategoryById(id: Long): Category? = categories[id]
        override suspend fun insertCategory(category: Category): Long {
            categories[category.id] = category
            return category.id
        }
        override suspend fun updateCategory(category: Category) {
            categories[category.id] = category
        }
        override suspend fun deleteCategory(id: Long) {
            categories.remove(id)
        }
    }

    @Before
    fun setUp() {
        fakeBudgetRepository = FakeBudgetRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        useCase = SaveBudgetUseCase(fakeBudgetRepository, fakeCategoryRepository)
    }

    @Test
    fun `when amount is zero or negative, return failure`() = runTest {
        val resultZero = useCase(categoryId = null, amount = 0.0, month = 9, year = 2026)
        assertTrue(resultZero.isFailure)

        val resultNegative = useCase(categoryId = null, amount = -100.0, month = 9, year = 2026)
        assertTrue(resultNegative.isFailure)
    }

    @Test
    fun `when month or year is invalid, return failure`() = runTest {
        val resultInvalidMonth = useCase(categoryId = null, amount = 1000.0, month = 13, year = 2026)
        assertTrue(resultInvalidMonth.isFailure)

        val resultInvalidYear = useCase(categoryId = null, amount = 1000.0, month = 9, year = 1999)
        assertTrue(resultInvalidYear.isFailure)
    }

    @Test
    fun `when saving valid global budget, repository is called and returns success`() = runTest {
        val result = useCase(categoryId = null, amount = 10000.0, month = 9, year = 2026)

        assertTrue(result.isSuccess)
        assertEquals(100L, result.getOrNull())
        val saved = fakeBudgetRepository.lastInsertedBudget
        assertEquals(null, saved?.categoryId)
        assertEquals(10000.0, saved?.amount ?: 0.0, 0.001)
        assertEquals(9, saved?.month)
        assertEquals(2026, saved?.year)
    }

    @Test
    fun `when category is an income category, return failure`() = runTest {
        val incomeCategory = Category(id = 5L, name = "راتب", type = CategoryType.INCOME, icon = "ic_salary", color = "#4CAF50")
        fakeCategoryRepository.insertCategory(incomeCategory)

        val result = useCase(categoryId = 5L, amount = 3000.0, month = 9, year = 2026)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(null, fakeBudgetRepository.lastInsertedBudget)
    }

    @Test
    fun `when category does not exist, return failure`() = runTest {
        val result = useCase(categoryId = 99L, amount = 3000.0, month = 9, year = 2026)

        assertTrue(result.isFailure)
        assertEquals(null, fakeBudgetRepository.lastInsertedBudget)
    }

    @Test
    fun `when saving valid expense category budget, repository is called and returns success`() = runTest {
        val expenseCategory = Category(id = 2L, name = "طعام", type = CategoryType.EXPENSE, icon = "ic_food", color = "#FF9800")
        fakeCategoryRepository.insertCategory(expenseCategory)

        val result = useCase(categoryId = 2L, amount = 4000.0, month = 9, year = 2026)

        assertTrue(result.isSuccess)
        assertEquals(100L, result.getOrNull())
        val saved = fakeBudgetRepository.lastInsertedBudget
        assertEquals(2L, saved?.categoryId)
        assertEquals(4000.0, saved?.amount ?: 0.0, 0.001)
        assertEquals(9, saved?.month)
        assertEquals(2026, saved?.year)
    }
}

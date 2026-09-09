package com.example.urwallet.features.budgets.domain.usecase

import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteBudgetUseCaseTest {

    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var useCase: DeleteBudgetUseCase

    private class FakeBudgetRepository : BudgetRepository {
        var deletedId: Long? = null
        override fun getGlobalBudget(month: Int, year: Int): Flow<Budget?> = flowOf(null)
        override fun getCategoryBudgets(month: Int, year: Int): Flow<List<Budget>> = flowOf(emptyList())
        override fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<Budget?> = flowOf(null)
        override suspend fun getGlobalBudgetSync(month: Int, year: Int): Budget? = null
        override suspend fun getBudgetForCategorySync(categoryId: Long, month: Int, year: Int): Budget? = null
        override suspend fun insertOrUpdateBudget(budget: Budget): Long = 1L
        override suspend fun deleteBudget(id: Long) {
            deletedId = id
        }
    }

    @Before
    fun setUp() {
        fakeBudgetRepository = FakeBudgetRepository()
        useCase = DeleteBudgetUseCase(fakeBudgetRepository)
    }

    @Test
    fun `when deleting budget, repository deleteBudget is called`() = runTest {
        val result = useCase(5L)

        assertTrue(result.isSuccess)
        assertEquals(5L, fakeBudgetRepository.deletedId)
    }
}

package com.example.urwallet.features.budgets.data.repository

import com.example.urwallet.features.budgets.data.dao.BudgetDao
import com.example.urwallet.features.budgets.data.entity.BudgetEntity
import com.example.urwallet.features.budgets.domain.model.Budget
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class BudgetRepositoryImplTest {

    private lateinit var fakeBudgetDao: FakeBudgetDao
    private lateinit var repository: BudgetRepositoryImpl

    private class FakeBudgetDao : BudgetDao {
        private val mutex = Mutex()
        val storage = mutableMapOf<Long, BudgetEntity>()
        private var idCounter = 1L

        override suspend fun insertBudget(budget: BudgetEntity): Long = mutex.withLock {
            val assignedId = if (budget.id > 0) budget.id else idCounter++
            val saved = budget.copy(id = assignedId)
            storage[assignedId] = saved
            assignedId
        }

        override suspend fun updateBudget(budget: BudgetEntity): Int = mutex.withLock {
            if (storage.containsKey(budget.id)) {
                storage[budget.id] = budget
                1
            } else {
                0
            }
        }

        override suspend fun getGlobalBudgetSync(month: Int, year: Int): BudgetEntity? = mutex.withLock {
            storage.values.firstOrNull { it.categoryId == null && it.month == month && it.year == year }
        }

        override suspend fun getBudgetForCategorySync(categoryId: Long, month: Int, year: Int): BudgetEntity? = mutex.withLock {
            storage.values.firstOrNull { it.categoryId == categoryId && it.month == month && it.year == year }
        }

        override fun getGlobalBudget(month: Int, year: Int): Flow<BudgetEntity?> {
            return flowOf(storage.values.firstOrNull { it.categoryId == null && it.month == month && it.year == year })
        }

        override fun getCategoryBudgets(month: Int, year: Int): Flow<List<BudgetEntity>> {
            return flowOf(storage.values.filter { it.categoryId != null && it.month == month && it.year == year })
        }

        override fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<BudgetEntity?> {
            return flowOf(storage.values.firstOrNull { it.categoryId == categoryId && it.month == month && it.year == year })
        }

        override suspend fun deleteBudget(id: Long): Int = mutex.withLock {
            if (storage.remove(id) != null) 1 else 0
        }

        override suspend fun insertBudgets(budgets: List<BudgetEntity>): List<Long> {
            return budgets.map { insertBudget(it) }
        }

        override suspend fun getAllBudgetsSync(): List<BudgetEntity> = mutex.withLock {
            storage.values.toList()
        }

        override suspend fun deleteAllBudgets(): Int = mutex.withLock {
            val count = storage.size
            storage.clear()
            count
        }
    }

    @Before
    fun setUp() {
        fakeBudgetDao = FakeBudgetDao()
        repository = BudgetRepositoryImpl(fakeBudgetDao)
    }

    @Test
    fun `insertOrUpdateBudget saves single global budget correctly`() = runTest {
        val globalBudget = Budget(
            id = 0,
            categoryId = null,
            amount = 5000.0,
            month = 9,
            year = 2026
        )

        val id = repository.insertOrUpdateBudget(globalBudget)
        assertEquals(1L, id)
        assertEquals(1, fakeBudgetDao.storage.size)

        val stored = fakeBudgetDao.storage[id]
        assertNotNull(stored)
        assertEquals(5000.0, stored!!.amount, 0.001)
        assertEquals(null, stored.categoryId)
    }

    @Test
    fun `insertOrUpdateBudget with second global budget updates existing record instead of creating duplicate`() = runTest {
        val firstBudget = Budget(
            id = 0,
            categoryId = null,
            amount = 5000.0,
            month = 9,
            year = 2026
        )
        val firstId = repository.insertOrUpdateBudget(firstBudget)

        val secondBudget = Budget(
            id = 0,
            categoryId = null,
            amount = 7500.0,
            month = 9,
            year = 2026
        )
        val secondId = repository.insertOrUpdateBudget(secondBudget)

        assertEquals(firstId, secondId)
        assertEquals(1, fakeBudgetDao.storage.size)
        val stored = fakeBudgetDao.storage[firstId]
        assertEquals(7500.0, stored!!.amount, 0.001)
    }

    @Test
    fun `multiple category budgets can exist for the same month and year`() = runTest {
        val cat1Budget = Budget(id = 0, categoryId = 1L, amount = 1000.0, month = 9, year = 2026)
        val cat2Budget = Budget(id = 0, categoryId = 2L, amount = 2000.0, month = 9, year = 2026)
        val globalBudget = Budget(id = 0, categoryId = null, amount = 10000.0, month = 9, year = 2026)

        val id1 = repository.insertOrUpdateBudget(cat1Budget)
        val id2 = repository.insertOrUpdateBudget(cat2Budget)
        val idGlobal = repository.insertOrUpdateBudget(globalBudget)

        assertEquals(3, fakeBudgetDao.storage.size)
        assertEquals(1000.0, fakeBudgetDao.storage[id1]!!.amount, 0.001)
        assertEquals(2000.0, fakeBudgetDao.storage[id2]!!.amount, 0.001)
        assertEquals(10000.0, fakeBudgetDao.storage[idGlobal]!!.amount, 0.001)
    }

    @Test
    fun `saving existing category budget updates amount and preserves categoryId`() = runTest {
        val catBudget = Budget(id = 0, categoryId = 1L, amount = 1000.0, month = 9, year = 2026)
        val id = repository.insertOrUpdateBudget(catBudget)

        val updatedCatBudget = Budget(id = 0, categoryId = 1L, amount = 1500.0, month = 9, year = 2026)
        val updatedId = repository.insertOrUpdateBudget(updatedCatBudget)

        assertEquals(id, updatedId)
        assertEquals(1, fakeBudgetDao.storage.size)
        assertEquals(1500.0, fakeBudgetDao.storage[id]!!.amount, 0.001)
        assertEquals(1L, fakeBudgetDao.storage[id]!!.categoryId)
    }

    @Test
    fun `concurrent or rapid saves for global budget maintain single record`() = runTest {
        val b1 = Budget(id = 0, categoryId = null, amount = 3000.0, month = 9, year = 2026)
        val b2 = Budget(id = 0, categoryId = null, amount = 4000.0, month = 9, year = 2026)

        val deferred1 = async { repository.insertOrUpdateBudget(b1) }
        val deferred2 = async { repository.insertOrUpdateBudget(b2) }

        val r1 = deferred1.await()
        val r2 = deferred2.await()

        assertEquals(r1, r2)
        assertEquals(1, fakeBudgetDao.storage.size)
    }
}

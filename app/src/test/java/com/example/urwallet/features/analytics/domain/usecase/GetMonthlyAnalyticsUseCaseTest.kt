package com.example.urwallet.features.analytics.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.analytics.domain.model.AnalyticsTimePeriod
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.more.domain.model.RecurringTransaction
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.data.entity.DailySpendingEntity
import com.example.urwallet.features.transactions.data.entity.MonthlyCashFlowEntity
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class GetMonthlyAnalyticsUseCaseTest {

    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var fakeRecurringRepository: FakeRecurringRepository
    private lateinit var useCase: GetMonthlyAnalyticsUseCase

    private class FakeTransactionRepository : TransactionRepository {
        var transactions = listOf<Transaction>()
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(transactions)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(transactions.find { it.id == id })
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(transactions.take(limit))
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(transactions.filter { it.date in startDate..endDate })
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(transactions.filter { it.categoryId == categoryId && it.date in startDate..endDate })
        override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> =
            flowOf(transactions.filter { it.type == type && it.date in startDate..endDate }.sumOf { it.amount })
        override fun getTotalSumByType(type: TransactionType): Flow<Double> =
            flowOf(transactions.filter { it.type == type }.sumOf { it.amount })
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> =
            flowOf(0.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(transactions.size)
        override suspend fun insertTransaction(transaction: Transaction): Long = 1L
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: Long): Category? = null
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}

        override fun getCategorySpendingBetween(startDate: Long, endDate: Long): Flow<Map<Long, Double>> =
            flowOf(transactions.filter { it.type == TransactionType.EXPENSE && it.date in startDate..endDate }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amount } })

        override fun getMonthlyCashFlowsBetween(startDate: Long, endDate: Long): Flow<List<MonthlyCashFlowEntity>> =
            flowOf(transactions.filter { it.date in startDate..endDate }
                .groupBy {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, it.type)
                }.map { (key, list) ->
                    MonthlyCashFlowEntity(key.first, key.second, key.third, list.sumOf { it.amount })
                })

        override fun getDailyExpensesBetween(startDate: Long, endDate: Long): Flow<List<DailySpendingEntity>> =
            flowOf(transactions.filter { it.type == TransactionType.EXPENSE && it.date in startDate..endDate }
                .groupBy {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                }.map { (key, list) ->
                    DailySpendingEntity(key.first, key.second, key.third, list.sumOf { it.amount })
                })
    }

    private class FakeCategoryRepository : CategoryRepository {
        var categories = listOf<Category>()
        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
            flowOf(categories.filter { it.type == type })
        override suspend fun getCategoryById(id: Long): Category? = categories.find { it.id == id }
        override suspend fun insertCategory(category: Category): Long = category.id
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    private class FakeBudgetRepository : BudgetRepository {
        var globalBudget: Budget? = null
        override fun getGlobalBudget(month: Int, year: Int): Flow<Budget?> = flowOf(globalBudget)
        override fun getCategoryBudgets(month: Int, year: Int): Flow<List<Budget>> = flowOf(emptyList())
        override fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<Budget?> = flowOf(null)
        override suspend fun getGlobalBudgetSync(month: Int, year: Int): Budget? = globalBudget
        override suspend fun getBudgetForCategorySync(categoryId: Long, month: Int, year: Int): Budget? = null
        override suspend fun insertOrUpdateBudget(budget: Budget): Long = 1L
        override suspend fun deleteBudget(id: Long) {}
    }

    private class FakeGoalRepository : GoalRepository {
        var activeGoals = listOf<Goal>()
        var monthlyContributions = 0.0
        override fun getAllGoals(): Flow<List<Goal>> = flowOf(activeGoals)
        override fun getActiveGoals(): Flow<List<Goal>> = flowOf(activeGoals)
        override fun getNearestActiveGoal(): Flow<Goal?> = flowOf(activeGoals.firstOrNull())
        override fun getGoalById(id: Long): Flow<Goal?> = flowOf(activeGoals.find { it.id == id })
        override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> = flowOf(emptyList())
        override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> = flowOf(monthlyContributions)
        override suspend fun insertGoal(goal: Goal): Long = 1L
        override suspend fun updateGoal(goal: Goal) {}
        override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long = 1L
        override suspend fun deleteGoal(id: Long) {}
    }

    private class FakeRecurringRepository : RecurringRepository {
        var recurring = listOf<RecurringTransaction>()
        override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(recurring)
        override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(recurring)
        override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long = 1L
        override suspend fun updateRecurringTransaction(recurring: RecurringTransaction) {}
        override suspend fun deleteRecurringTransaction(id: Long) {}
        override suspend fun toggleActive(id: Long, isActive: Boolean) {}
    }

    @Before
    fun setUp() {
        fakeTransactionRepository = FakeTransactionRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakeBudgetRepository = FakeBudgetRepository()
        fakeGoalRepository = FakeGoalRepository()
        fakeRecurringRepository = FakeRecurringRepository()

        useCase = GetMonthlyAnalyticsUseCase(
            transactionRepository = fakeTransactionRepository,
            categoryRepository = fakeCategoryRepository,
            budgetRepository = fakeBudgetRepository,
            goalRepository = fakeGoalRepository,
            recurringRepository = fakeRecurringRepository
        )
    }

    @Test
    fun `when month has no transactions, result reflects zero amounts and hasData is false`() = runTest {
        val result = useCase(month = 9, year = 2026).first()

        assertEquals(0.0, result.totalIncome, 0.001)
        assertEquals(0.0, result.totalExpenses, 0.001)
        assertEquals(0.0, result.netSavings, 0.001)
        assertEquals(0.0, result.savingsRate, 0.001)
        assertEquals(0.0, result.expenseToIncomeRatio, 0.001)
        assertTrue(result.categoryBreakdown.isEmpty())
        assertFalse(result.hasData)
    }

    @Test
    fun `evaluates income, expenses, and net savings correctly`() = runTest {
        val calDay5 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 5, 12, 0) }
        val calDay12 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 12, 12, 0) }

        val catFood = Category(id = 1L, name = "طعام", type = CategoryType.EXPENSE, icon = "ic_food", color = "#FF7043")
        val catBills = Category(id = 2L, name = "فواتير", type = CategoryType.EXPENSE, icon = "ic_bills", color = "#FFA726")
        fakeCategoryRepository.categories = listOf(catFood, catBills)

        val transactions = listOf(
            Transaction(id = 1L, amount = 10000.0, type = TransactionType.INCOME, categoryId = 99L, title = "راتب", date = calDay5.timeInMillis),
            Transaction(id = 2L, amount = 3000.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "سوبرماركت", date = calDay5.timeInMillis),
            Transaction(id = 3L, amount = 1000.0, type = TransactionType.EXPENSE, categoryId = 2L, title = "كهرباء", date = calDay12.timeInMillis)
        )
        fakeTransactionRepository.transactions = transactions

        val result = useCase(month = 9, year = 2026).first()

        assertEquals(10000.0, result.totalIncome, 0.001)
        assertEquals(4000.0, result.totalExpenses, 0.001)
        assertEquals(6000.0, result.netSavings, 0.001)
        assertEquals(60.0, result.savingsRate, 0.001) // (6000 / 10000) * 100
        assertEquals(40.0, result.expenseToIncomeRatio, 0.001) // (4000 / 10000) * 100
        assertTrue(result.hasData)

        // Verify Daily trend has expenses on Day 5 and Day 12
        assertEquals(3000.0, result.dailySpending[5] ?: 0.0, 0.001)
        assertEquals(1000.0, result.dailySpending[12] ?: 0.0, 0.001)
        assertEquals(0.0, result.dailySpending[1] ?: 0.0, 0.001)

        // Verify Category Breakdown sorted descending
        assertEquals(2, result.categoryBreakdown.size)
        assertEquals("طعام", result.categoryBreakdown[0].categoryName)
        assertEquals(3000.0, result.categoryBreakdown[0].amount, 0.001)
        assertEquals(75.0, result.categoryBreakdown[0].percentage, 0.001) // 3000 / 4000

        assertEquals("فواتير", result.categoryBreakdown[1].categoryName)
        assertEquals(1000.0, result.categoryBreakdown[1].amount, 0.001)
        assertEquals(25.0, result.categoryBreakdown[1].percentage, 0.001) // 1000 / 4000
    }

    @Test
    fun `when expenses only and zero income, net savings is negative and savings rate is 0`() = runTest {
        val calDay10 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 10, 12, 0) }
        val catFood = Category(id = 1L, name = "طعام", type = CategoryType.EXPENSE, icon = "ic_food", color = "#FF7043")
        fakeCategoryRepository.categories = listOf(catFood)

        fakeTransactionRepository.transactions = listOf(
            Transaction(id = 1L, amount = 2500.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "أكل", date = calDay10.timeInMillis)
        )

        val result = useCase(month = 9, year = 2026).first()

        assertEquals(0.0, result.totalIncome, 0.001)
        assertEquals(2500.0, result.totalExpenses, 0.001)
        assertEquals(-2500.0, result.netSavings, 0.001)
        assertEquals(0.0, result.savingsRate, 0.001)
        assertEquals(100.0, result.expenseToIncomeRatio, 0.001)
        assertEquals(1, result.categoryBreakdown.size)
        assertEquals(100.0, result.categoryBreakdown[0].percentage, 0.001)
    }

    @Test
    fun `when income only and zero expenses, savings rate is 100 percent and category percentage is 0`() = runTest {
        val calDay1 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 1, 12, 0) }

        fakeTransactionRepository.transactions = listOf(
            Transaction(id = 1L, amount = 8000.0, type = TransactionType.INCOME, categoryId = 99L, title = "راتب", date = calDay1.timeInMillis)
        )

        val result = useCase(month = 9, year = 2026).first()

        assertEquals(8000.0, result.totalIncome, 0.001)
        assertEquals(0.0, result.totalExpenses, 0.001)
        assertEquals(8000.0, result.netSavings, 0.001)
        assertEquals(100.0, result.savingsRate, 0.001)
        assertEquals(0.0, result.expenseToIncomeRatio, 0.001)
        assertTrue(result.categoryBreakdown.isEmpty())
    }

    @Test
    fun `transactions outside the target period are excluded`() = runTest {
        val calInPeriod = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 15, 12, 0) }
        val calOutsidePeriod = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 20, 12, 0) }

        val catFood = Category(id = 1L, name = "طعام", type = CategoryType.EXPENSE, icon = "ic_food", color = "#FF7043")
        fakeCategoryRepository.categories = listOf(catFood)

        fakeTransactionRepository.transactions = listOf(
            Transaction(id = 1L, amount = 1200.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "داخل الشهر", date = calInPeriod.timeInMillis),
            Transaction(id = 2L, amount = 5000.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "خارج الشهر", date = calOutsidePeriod.timeInMillis)
        )

        val result = useCase(month = 9, year = 2026).first()

        assertEquals(1200.0, result.totalExpenses, 0.001)
        assertEquals(1, result.categoryBreakdown.size)
        assertEquals(1200.0, result.categoryBreakdown[0].amount, 0.001)
    }

    @Test
    fun `multiple transactions in same category are summed accurately`() = runTest {
        val calDay1 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 2, 12, 0) }
        val calDay2 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 8, 12, 0) }
        val calDay3 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 20, 12, 0) }

        val catCoffee = Category(id = 5L, name = "قهوة", type = CategoryType.EXPENSE, icon = "ic_coffee", color = "#6D4C41")
        fakeCategoryRepository.categories = listOf(catCoffee)

        fakeTransactionRepository.transactions = listOf(
            Transaction(id = 1L, amount = 50.0, type = TransactionType.EXPENSE, categoryId = 5L, title = "لاتيه", date = calDay1.timeInMillis),
            Transaction(id = 2L, amount = 75.0, type = TransactionType.EXPENSE, categoryId = 5L, title = "كابتشينو", date = calDay2.timeInMillis),
            Transaction(id = 3L, amount = 125.0, type = TransactionType.EXPENSE, categoryId = 5L, title = "إسبريسو", date = calDay3.timeInMillis)
        )

        val result = useCase(month = 9, year = 2026).first()

        assertEquals(250.0, result.totalExpenses, 0.001)
        assertEquals(1, result.categoryBreakdown.size)
        assertEquals(250.0, result.categoryBreakdown[0].amount, 0.001)
        assertEquals(100.0, result.categoryBreakdown[0].percentage, 0.001)
    }

    @Test
    fun `goal contributions are not counted as expenses and are exposed separately`() = runTest {
        val calDay10 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 10, 12, 0) }

        val catBills = Category(id = 2L, name = "فواتير", type = CategoryType.EXPENSE, icon = "ic_bills", color = "#FFA726")
        fakeCategoryRepository.categories = listOf(catBills)

        fakeTransactionRepository.transactions = listOf(
            Transaction(id = 1L, amount = 5000.0, type = TransactionType.INCOME, categoryId = 99L, title = "دخل", date = calDay10.timeInMillis),
            Transaction(id = 2L, amount = 1500.0, type = TransactionType.EXPENSE, categoryId = 2L, title = "كهرباء", date = calDay10.timeInMillis)
        )
        fakeGoalRepository.monthlyContributions = 1000.0

        val result = useCase(month = 9, year = 2026).first()

        // Goal contribution is NOT in totalExpenses
        assertEquals(1500.0, result.totalExpenses, 0.001)
        assertEquals(3500.0, result.netSavings, 0.001)
        // Goal contribution is exposed separately
        assertEquals(1000.0, result.totalGoalSavings, 0.001)
    }

    @Test
    fun `supports time periods like THIS_MONTH and LAST_MONTH`() = runTest {
        val calThisMonth = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 10, 12, 0) }
        val calLastMonth = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 10, 12, 0) }

        val catGeneral = Category(id = 10L, name = "عام", type = CategoryType.EXPENSE, icon = "ic_other", color = "#90A4AE")
        fakeCategoryRepository.categories = listOf(catGeneral)

        fakeTransactionRepository.transactions = listOf(
            Transaction(id = 1L, amount = 3000.0, type = TransactionType.EXPENSE, categoryId = 10L, title = "هذا الشهر", date = calThisMonth.timeInMillis),
            Transaction(id = 2L, amount = 4500.0, type = TransactionType.EXPENSE, categoryId = 10L, title = "الشهر الماضي", date = calLastMonth.timeInMillis)
        )

        val thisMonthResult = useCase(AnalyticsTimePeriod.THIS_MONTH, month = 9, year = 2026).first()
        assertEquals(3000.0, thisMonthResult.totalExpenses, 0.001)

        val lastMonthResult = useCase(AnalyticsTimePeriod.LAST_MONTH, month = 9, year = 2026).first()
        assertEquals(4500.0, lastMonthResult.totalExpenses, 0.001)
    }

    @Test
    fun `budget utilization reflects ratio of expenses to global budget`() = runTest {
        val calDay5 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 5, 12, 0) }
        val catFood = Category(id = 1L, name = "طعام", type = CategoryType.EXPENSE, icon = "ic_food", color = "#FF7043")
        fakeCategoryRepository.categories = listOf(catFood)

        fakeTransactionRepository.transactions = listOf(
            Transaction(id = 1L, amount = 2500.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "أكل", date = calDay5.timeInMillis)
        )
        fakeBudgetRepository.globalBudget = Budget(id = 1L, categoryId = null, amount = 5000.0, month = 9, year = 2026)

        val result = useCase(month = 9, year = 2026).first()

        assertEquals(50.0, result.budgetUtilization, 0.001) // 2500 / 5000 * 100
    }
}

package com.example.urwallet.features.notifications

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import com.example.urwallet.features.notifications.domain.model.NotificationSettings
import com.example.urwallet.features.notifications.domain.repository.NotificationRepository
import com.example.urwallet.features.notifications.domain.usecase.CheckBudgetAlertUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckBudgetAlertUseCaseTest {

    private lateinit var fakeBudgetRepo: FakeBudgetRepository
    private lateinit var fakeTransactionRepo: FakeTransactionRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var fakeNotificationRepo: FakeNotificationRepository
    private lateinit var fakeNotificationHelper: FakeNotificationHelper
    private lateinit var checkBudgetAlertUseCase: CheckBudgetAlertUseCase

    private val currentMonth = DateUtils.getCurrentMonth()
    private val currentYear = DateUtils.getCurrentYear()

    @Before
    fun setup() {
        fakeBudgetRepo = FakeBudgetRepository()
        fakeTransactionRepo = FakeTransactionRepository()
        fakeCategoryRepo = FakeCategoryRepository()
        fakeNotificationRepo = FakeNotificationRepository()
        fakeNotificationHelper = FakeNotificationHelper()

        checkBudgetAlertUseCase = CheckBudgetAlertUseCase(
            budgetRepository = fakeBudgetRepo,
            transactionRepository = fakeTransactionRepo,
            categoryRepository = fakeCategoryRepo,
            notificationRepository = fakeNotificationRepo,
            notificationHelper = fakeNotificationHelper
        )
    }

    @Test
    fun categoryBudget_thresholdCrossing_79_to_80_sendsWarningNotification() = runTest {
        val categoryId = 10L
        fakeCategoryRepo.categories[categoryId] = Category(id = categoryId, name = "طعام ومشروبات", icon = "ic_food", color = "#FF5722", type = CategoryType.EXPENSE)
        fakeBudgetRepo.categoryBudgets[categoryId] = Budget(id = 1L, categoryId = categoryId, amount = 1000.0, month = currentMonth, year = currentYear)

        // Previous spent = 790, new transaction = 10 -> current spent = 800 (80%)
        fakeTransactionRepo.spentByCategory[categoryId] = 800.0

        checkBudgetAlertUseCase(categoryId = categoryId, amount = 10.0)

        assertEquals(1, fakeNotificationHelper.dispatchedBudgetAlerts.size)
        val alert = fakeNotificationHelper.dispatchedBudgetAlerts.first()
        assertEquals("طعام ومشروبات", alert.budgetName)
        assertEquals(80, alert.percentage)
        assertEquals(false, alert.isExceeded)
        assertEquals(categoryId, alert.categoryId)
    }

    @Test
    fun categoryBudget_thresholdCrossing_79_to_81_sendsWarningNotification() = runTest {
        val categoryId = 10L
        fakeCategoryRepo.categories[categoryId] = Category(id = categoryId, name = "طعام ومشروبات", icon = "ic_food", color = "#FF5722", type = CategoryType.EXPENSE)
        fakeBudgetRepo.categoryBudgets[categoryId] = Budget(id = 1L, categoryId = categoryId, amount = 1000.0, month = currentMonth, year = currentYear)

        // Previous spent = 790, new transaction = 20 -> current spent = 810 (81%)
        fakeTransactionRepo.spentByCategory[categoryId] = 810.0

        checkBudgetAlertUseCase(categoryId = categoryId, amount = 20.0)

        assertEquals(1, fakeNotificationHelper.dispatchedBudgetAlerts.size)
        val alert = fakeNotificationHelper.dispatchedBudgetAlerts.first()
        assertEquals(80, alert.percentage)
        assertEquals(false, alert.isExceeded)
    }

    @Test
    fun categoryBudget_thresholdNotCrossing_80_to_85_doesNotSendDuplicate() = runTest {
        val categoryId = 10L
        fakeCategoryRepo.categories[categoryId] = Category(id = categoryId, name = "طعام ومشروبات", icon = "ic_food", color = "#FF5722", type = CategoryType.EXPENSE)
        fakeBudgetRepo.categoryBudgets[categoryId] = Budget(id = 1L, categoryId = categoryId, amount = 1000.0, month = currentMonth, year = currentYear)

        // Previous spent was already 800 (80%), new transaction = 50 -> current spent = 850 (85%)
        fakeTransactionRepo.spentByCategory[categoryId] = 850.0

        checkBudgetAlertUseCase(categoryId = categoryId, amount = 50.0)

        // Previous was already >= 80%, so 80% threshold was NOT crossed by this transaction
        assertEquals(0, fakeNotificationHelper.dispatchedBudgetAlerts.size)
    }

    @Test
    fun categoryBudget_thresholdCrossing_99_to_100_sendsExceededNotification() = runTest {
        val categoryId = 10L
        fakeCategoryRepo.categories[categoryId] = Category(id = categoryId, name = "مواصلات", icon = "ic_transport", color = "#2196F3", type = CategoryType.EXPENSE)
        fakeBudgetRepo.categoryBudgets[categoryId] = Budget(id = 2L, categoryId = categoryId, amount = 500.0, month = currentMonth, year = currentYear)

        // Previous spent = 495 (99%), new transaction = 5 -> current spent = 500 (100%)
        fakeTransactionRepo.spentByCategory[categoryId] = 500.0

        checkBudgetAlertUseCase(categoryId = categoryId, amount = 5.0)

        assertEquals(1, fakeNotificationHelper.dispatchedBudgetAlerts.size)
        val alert = fakeNotificationHelper.dispatchedBudgetAlerts.first()
        assertEquals("مواصلات", alert.budgetName)
        assertEquals(100, alert.percentage)
        assertEquals(true, alert.isExceeded)
    }

    @Test
    fun categoryBudget_thresholdCrossing_99_to_101_sendsExceededNotification() = runTest {
        val categoryId = 10L
        fakeCategoryRepo.categories[categoryId] = Category(id = categoryId, name = "مواصلات", icon = "ic_transport", color = "#2196F3", type = CategoryType.EXPENSE)
        fakeBudgetRepo.categoryBudgets[categoryId] = Budget(id = 2L, categoryId = categoryId, amount = 500.0, month = currentMonth, year = currentYear)

        // Previous spent = 495 (99%), new transaction = 10 -> current spent = 505 (101%)
        fakeTransactionRepo.spentByCategory[categoryId] = 505.0

        checkBudgetAlertUseCase(categoryId = categoryId, amount = 10.0)

        assertEquals(1, fakeNotificationHelper.dispatchedBudgetAlerts.size)
        val alert = fakeNotificationHelper.dispatchedBudgetAlerts.first()
        assertEquals(100, alert.percentage)
        assertEquals(true, alert.isExceeded)
    }

    @Test
    fun categoryBudget_thresholdNotCrossing_100_to_110_doesNotSendDuplicate() = runTest {
        val categoryId = 10L
        fakeCategoryRepo.categories[categoryId] = Category(id = categoryId, name = "مواصلات", icon = "ic_transport", color = "#2196F3", type = CategoryType.EXPENSE)
        fakeBudgetRepo.categoryBudgets[categoryId] = Budget(id = 2L, categoryId = categoryId, amount = 500.0, month = currentMonth, year = currentYear)

        // Previous spent was already 500 (100%), new transaction = 50 -> current spent = 550 (110%)
        fakeTransactionRepo.spentByCategory[categoryId] = 550.0

        checkBudgetAlertUseCase(categoryId = categoryId, amount = 50.0)

        // Previous was already >= 100%, so 100% threshold was NOT crossed
        assertEquals(0, fakeNotificationHelper.dispatchedBudgetAlerts.size)
    }

    @Test
    fun globalBudget_thresholdCrossing_sendsGlobalNotification() = runTest {
        fakeBudgetRepo.globalBudget = Budget(id = 99L, categoryId = null, amount = 10000.0, month = currentMonth, year = currentYear)

        // Previous = 7900 (79%), new = 100 -> current = 8000 (80%)
        fakeTransactionRepo.totalExpenseSpent = 8000.0

        checkBudgetAlertUseCase(categoryId = 0L, amount = 100.0)

        assertEquals(1, fakeNotificationHelper.dispatchedBudgetAlerts.size)
        val alert = fakeNotificationHelper.dispatchedBudgetAlerts.first()
        assertEquals("الميزانية العامة", alert.budgetName)
        assertEquals(80, alert.percentage)
        assertEquals(false, alert.isExceeded)
        assertEquals(null, alert.categoryId)
    }

    @Test
    fun alertsDisabled_doesNotSendNotification() = runTest {
        fakeNotificationRepo.setBudgetAlertsEnabled(false)

        val categoryId = 10L
        fakeCategoryRepo.categories[categoryId] = Category(id = categoryId, name = "طعام", icon = "ic_food", color = "#FF5722", type = CategoryType.EXPENSE)
        fakeBudgetRepo.categoryBudgets[categoryId] = Budget(id = 1L, categoryId = categoryId, amount = 1000.0, month = currentMonth, year = currentYear)
        fakeTransactionRepo.spentByCategory[categoryId] = 800.0

        checkBudgetAlertUseCase(categoryId = categoryId, amount = 10.0)

        assertTrue(fakeNotificationHelper.dispatchedBudgetAlerts.isEmpty())
    }

    @Test
    fun idempotency_repeatedCallForDeliveredAlert_doesNotSendAgain() = runTest {
        val categoryId = 10L
        fakeCategoryRepo.categories[categoryId] = Category(id = categoryId, name = "طعام", icon = "ic_food", color = "#FF5722", type = CategoryType.EXPENSE)
        fakeBudgetRepo.categoryBudgets[categoryId] = Budget(id = 1L, categoryId = categoryId, amount = 1000.0, month = currentMonth, year = currentYear)
        fakeTransactionRepo.spentByCategory[categoryId] = 800.0

        // First call sends notification and records delivery key
        checkBudgetAlertUseCase(categoryId = categoryId, amount = 10.0)
        assertEquals(1, fakeNotificationHelper.dispatchedBudgetAlerts.size)

        // Repeated call with same state (e.g. process restart / repeat)
        checkBudgetAlertUseCase(categoryId = categoryId, amount = 10.0)
        assertEquals(1, fakeNotificationHelper.dispatchedBudgetAlerts.size)
    }

    // --- Fake Test Implementations ---

    data class DispatchedBudgetAlert(
        val budgetName: String,
        val percentage: Int,
        val isExceeded: Boolean,
        val categoryId: Long?
    )

    private class FakeNotificationHelper : NotificationHelper() {
        val dispatchedBudgetAlerts = mutableListOf<DispatchedBudgetAlert>()

        override fun hasNotificationPermission(): Boolean = true

        override fun showBudgetAlertNotification(
            budgetName: String,
            percentage: Int,
            isExceeded: Boolean,
            categoryId: Long?
        ) {
            dispatchedBudgetAlerts.add(
                DispatchedBudgetAlert(budgetName, percentage, isExceeded, categoryId)
            )
        }
    }

    private class FakeNotificationRepository : NotificationRepository {
        private val _settings = MutableStateFlow(NotificationSettings())
        private val deliveredKeys = mutableSetOf<String>()

        override fun getNotificationSettings(): Flow<NotificationSettings> = _settings

        override suspend fun setDailyReminderEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(isDailyReminderEnabled = enabled)
        }

        override suspend fun setReminderTime(hour: Int, minute: Int) {
            _settings.value = _settings.value.copy(reminderHour = hour, reminderMinute = minute)
        }

        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(isBudgetAlertsEnabled = enabled)
        }

        override suspend fun setGoalAlertsEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(isGoalAlertsEnabled = enabled)
        }

        override suspend fun markAlertDelivered(key: String) {
            deliveredKeys.add(key)
        }

        override suspend fun isAlertDelivered(key: String): Boolean {
            return deliveredKeys.contains(key)
        }

        override fun scheduleDailyReminder(hour: Int, minute: Int) {}
        override fun cancelDailyReminder() {}
    }

    private class FakeBudgetRepository : BudgetRepository {
        var globalBudget: Budget? = null
        val categoryBudgets = mutableMapOf<Long, Budget>()

        override fun getGlobalBudget(month: Int, year: Int): Flow<Budget?> = flowOf(globalBudget)
        override fun getCategoryBudgets(month: Int, year: Int): Flow<List<Budget>> = flowOf(categoryBudgets.values.toList())
        override fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<Budget?> = flowOf(categoryBudgets[categoryId])
        override suspend fun getGlobalBudgetSync(month: Int, year: Int): Budget? = globalBudget
        override suspend fun getBudgetForCategorySync(categoryId: Long, month: Int, year: Int): Budget? = categoryBudgets[categoryId]
        override suspend fun insertOrUpdateBudget(budget: Budget): Long = 1L
        override suspend fun deleteBudget(id: Long) {}
    }

    private class FakeTransactionRepository : TransactionRepository {
        val spentByCategory = mutableMapOf<Long, Double>()
        var totalExpenseSpent = 0.0

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(null)
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(totalExpenseSpent)
        override fun getTotalSumByType(type: TransactionType): Flow<Double> = flowOf(totalExpenseSpent)
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> =
            flowOf(spentByCategory[categoryId] ?: 0.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(0)
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
    }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = mutableMapOf<Long, Category>()

        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories.values.toList())
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = flowOf(categories.values.filter { it.type == type })
        override suspend fun getCategoryById(id: Long): Category? = categories[id]
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }
}

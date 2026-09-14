package com.example.urwallet.features.notifications.domain.usecase

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import com.example.urwallet.features.notifications.domain.repository.NotificationRepository
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class CheckBudgetAlertUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationHelper: NotificationHelper
) {

    suspend operator fun invoke(
        categoryId: Long,
        amount: Double,
        date: Long = System.currentTimeMillis()
    ) {
        try {
            val settings = notificationRepository.getNotificationSettings().first()
            if (!settings.isBudgetAlertsEnabled) return

            val calendar = Calendar.getInstance().apply { timeInMillis = date }
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val startOfMonth = DateUtils.getStartOfMonth(month, year)
            val endOfMonth = DateUtils.getEndOfMonth(month, year)

            // 1. Evaluate Category Budget
            if (categoryId > 0L) {
                val categoryBudget = budgetRepository.getBudgetForCategorySync(categoryId, month, year)
                if (categoryBudget != null && categoryBudget.amount > 0.0) {
                    val currentSpent = transactionRepository.getSumByCategoryAndPeriod(categoryId, startOfMonth, endOfMonth).first()
                    val prevSpent = (currentSpent - amount).coerceAtLeast(0.0)
                    val limit = categoryBudget.amount
                    val prevRatio = prevSpent / limit
                    val currRatio = currentSpent / limit
                    val categoryName = categoryRepository.getCategoryById(categoryId)?.name ?: "الميزانية"

                    if (prevRatio < 1.0 && currRatio >= 1.0) {
                        val key = "budget_cat_100_${categoryId}_${year}_${month}"
                        if (!notificationRepository.isAlertDelivered(key)) {
                            notificationRepository.markAlertDelivered(key)
                            notificationHelper.showBudgetAlertNotification(
                                budgetName = categoryName,
                                percentage = 100,
                                isExceeded = true,
                                categoryId = categoryId
                            )
                        }
                    } else if (prevRatio < 0.8 && currRatio >= 0.8) {
                        val key = "budget_cat_80_${categoryId}_${year}_${month}"
                        if (!notificationRepository.isAlertDelivered(key)) {
                            notificationRepository.markAlertDelivered(key)
                            notificationHelper.showBudgetAlertNotification(
                                budgetName = categoryName,
                                percentage = 80,
                                isExceeded = false,
                                categoryId = categoryId
                            )
                        }
                    }
                }
            }

            // 2. Evaluate Global Budget
            val globalBudget = budgetRepository.getGlobalBudgetSync(month, year)
            if (globalBudget != null && globalBudget.amount > 0.0) {
                val currentGlobalSpent = transactionRepository.getSumByTypeAndPeriod(TransactionType.EXPENSE, startOfMonth, endOfMonth).first()
                val prevGlobalSpent = (currentGlobalSpent - amount).coerceAtLeast(0.0)
                val globalLimit = globalBudget.amount
                val prevRatio = prevGlobalSpent / globalLimit
                val currRatio = currentGlobalSpent / globalLimit

                if (prevRatio < 1.0 && currRatio >= 1.0) {
                    val key = "budget_global_100_${year}_${month}"
                    if (!notificationRepository.isAlertDelivered(key)) {
                        notificationRepository.markAlertDelivered(key)
                        notificationHelper.showBudgetAlertNotification(
                            budgetName = "الميزانية العامة",
                            percentage = 100,
                            isExceeded = true,
                            categoryId = null
                        )
                    }
                } else if (prevRatio < 0.8 && currRatio >= 0.8) {
                    val key = "budget_global_80_${year}_${month}"
                    if (!notificationRepository.isAlertDelivered(key)) {
                        notificationRepository.markAlertDelivered(key)
                        notificationHelper.showBudgetAlertNotification(
                            budgetName = "الميزانية العامة",
                            percentage = 80,
                            isExceeded = false,
                            categoryId = null
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Notifications failure must never break the financial transaction
        }
    }
}

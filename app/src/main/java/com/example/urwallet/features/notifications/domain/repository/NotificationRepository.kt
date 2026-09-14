package com.example.urwallet.features.notifications.domain.repository

import com.example.urwallet.features.notifications.domain.model.NotificationSettings
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository abstraction for notification preferences and alarm scheduling.
 */
interface NotificationRepository {
    fun getNotificationSettings(): Flow<NotificationSettings>
    suspend fun setDailyReminderEnabled(enabled: Boolean)
    suspend fun setReminderTime(hour: Int, minute: Int)
    suspend fun setBudgetAlertsEnabled(enabled: Boolean)
    suspend fun setGoalAlertsEnabled(enabled: Boolean)
    suspend fun markAlertDelivered(key: String)
    suspend fun isAlertDelivered(key: String): Boolean
    fun scheduleDailyReminder(hour: Int, minute: Int)
    fun cancelDailyReminder()
}

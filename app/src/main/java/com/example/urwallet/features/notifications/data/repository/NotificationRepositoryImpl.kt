package com.example.urwallet.features.notifications.data.repository

import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.features.notifications.data.scheduler.AlarmScheduler
import com.example.urwallet.features.notifications.domain.model.NotificationSettings
import com.example.urwallet.features.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val appPreferences: AppPreferences,
    private val alarmScheduler: AlarmScheduler
) : NotificationRepository {

    override fun getNotificationSettings(): Flow<NotificationSettings> {
        return combine(
            appPreferences.isDailyReminderEnabled,
            appPreferences.reminderHour,
            appPreferences.reminderMinute,
            appPreferences.isBudgetAlertsEnabled,
            appPreferences.isGoalAlertsEnabled
        ) { dailyEnabled, hour, minute, budgetEnabled, goalEnabled ->
            NotificationSettings(
                isDailyReminderEnabled = dailyEnabled,
                reminderHour = hour,
                reminderMinute = minute,
                isBudgetAlertsEnabled = budgetEnabled,
                isGoalAlertsEnabled = goalEnabled
            )
        }
    }

    override suspend fun setDailyReminderEnabled(enabled: Boolean) {
        appPreferences.setDailyReminderEnabled(enabled)
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        appPreferences.setReminderTime(hour, minute)
    }

    override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        appPreferences.setBudgetAlertsEnabled(enabled)
    }

    override suspend fun setGoalAlertsEnabled(enabled: Boolean) {
        appPreferences.setGoalAlertsEnabled(enabled)
    }

    override suspend fun markAlertDelivered(key: String) {
        appPreferences.markAlertDelivered(key)
    }

    override suspend fun isAlertDelivered(key: String): Boolean {
        return appPreferences.isAlertDelivered(key)
    }

    override fun scheduleDailyReminder(hour: Int, minute: Int) {
        alarmScheduler.scheduleDailyReminder(hour, minute)
    }

    override fun cancelDailyReminder() {
        alarmScheduler.cancelDailyReminder()
    }
}

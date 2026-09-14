package com.example.urwallet.features.notifications.domain.usecase

import com.example.urwallet.features.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateNotificationSettingsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        notificationRepository.setDailyReminderEnabled(enabled)
        if (enabled) {
            val settings = notificationRepository.getNotificationSettings().first()
            notificationRepository.scheduleDailyReminder(settings.reminderHour, settings.reminderMinute)
        } else {
            notificationRepository.cancelDailyReminder()
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        notificationRepository.setReminderTime(hour, minute)
        val settings = notificationRepository.getNotificationSettings().first()
        if (settings.isDailyReminderEnabled) {
            notificationRepository.scheduleDailyReminder(hour, minute)
        }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        notificationRepository.setBudgetAlertsEnabled(enabled)
    }

    suspend fun setGoalAlertsEnabled(enabled: Boolean) {
        notificationRepository.setGoalAlertsEnabled(enabled)
    }
}

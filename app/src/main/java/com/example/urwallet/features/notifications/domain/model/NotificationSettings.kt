package com.example.urwallet.features.notifications.domain.model

/**
 * Domain model representing user-configurable notification settings.
 */
data class NotificationSettings(
    val isDailyReminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val isBudgetAlertsEnabled: Boolean = true,
    val isGoalAlertsEnabled: Boolean = true
)

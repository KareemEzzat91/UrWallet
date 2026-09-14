package com.example.urwallet.features.notifications.presentation

import com.example.urwallet.features.notifications.domain.model.NotificationSettings

data class NotificationSettingsUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val formattedReminderTime: String = "09:00 م",
    val isPermissionGranted: Boolean = true,
    val showPermissionBanner: Boolean = false,
    val isExactAlarmPermitted: Boolean = true,
    val testNotificationMessage: String? = null
)

package com.example.urwallet.features.notifications.domain.usecase

import com.example.urwallet.features.notifications.domain.model.NotificationSettings
import com.example.urwallet.features.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotificationSettingsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(): Flow<NotificationSettings> {
        return notificationRepository.getNotificationSettings()
    }
}

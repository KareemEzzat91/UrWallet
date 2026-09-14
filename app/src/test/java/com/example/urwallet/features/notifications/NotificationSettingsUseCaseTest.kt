package com.example.urwallet.features.notifications

import com.example.urwallet.features.notifications.domain.model.NotificationSettings
import com.example.urwallet.features.notifications.domain.repository.NotificationRepository
import com.example.urwallet.features.notifications.domain.usecase.GetNotificationSettingsUseCase
import com.example.urwallet.features.notifications.domain.usecase.UpdateNotificationSettingsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationSettingsUseCaseTest {

    private lateinit var fakeRepository: FakeNotificationRepository
    private lateinit var getSettingsUseCase: GetNotificationSettingsUseCase
    private lateinit var updateSettingsUseCase: UpdateNotificationSettingsUseCase

    @Before
    fun setup() {
        fakeRepository = FakeNotificationRepository()
        getSettingsUseCase = GetNotificationSettingsUseCase(fakeRepository)
        updateSettingsUseCase = UpdateNotificationSettingsUseCase(fakeRepository)
    }

    @Test
    fun getSettings_returnsDefaultValues() = runTest {
        val settings = getSettingsUseCase().first()
        assertTrue(settings.isDailyReminderEnabled)
        assertEquals(21, settings.reminderHour)
        assertEquals(0, settings.reminderMinute)
        assertTrue(settings.isBudgetAlertsEnabled)
        assertTrue(settings.isGoalAlertsEnabled)
    }

    @Test
    fun setDailyReminderEnabled_false_cancelsAlarm() = runTest {
        updateSettingsUseCase.setDailyReminderEnabled(false)

        val settings = getSettingsUseCase().first()
        assertFalse(settings.isDailyReminderEnabled)
        assertTrue(fakeRepository.isAlarmCancelled)
    }

    @Test
    fun setDailyReminderEnabled_true_schedulesAlarm() = runTest {
        // First disable
        updateSettingsUseCase.setDailyReminderEnabled(false)
        fakeRepository.scheduledHour = null
        fakeRepository.scheduledMinute = null

        // Then enable
        updateSettingsUseCase.setDailyReminderEnabled(true)

        val settings = getSettingsUseCase().first()
        assertTrue(settings.isDailyReminderEnabled)
        assertEquals(21, fakeRepository.scheduledHour)
        assertEquals(0, fakeRepository.scheduledMinute)
    }

    @Test
    fun setReminderTime_whenEnabled_updatesPreferenceAndSchedulesAlarm() = runTest {
        updateSettingsUseCase.setReminderTime(hour = 20, minute = 30)

        val settings = getSettingsUseCase().first()
        assertEquals(20, settings.reminderHour)
        assertEquals(30, settings.reminderMinute)
        assertEquals(20, fakeRepository.scheduledHour)
        assertEquals(30, fakeRepository.scheduledMinute)
    }

    @Test
    fun setReminderTime_whenDisabled_updatesPreferenceWithoutScheduling() = runTest {
        updateSettingsUseCase.setDailyReminderEnabled(false)
        fakeRepository.scheduledHour = null
        fakeRepository.scheduledMinute = null

        updateSettingsUseCase.setReminderTime(hour = 19, minute = 15)

        val settings = getSettingsUseCase().first()
        assertEquals(19, settings.reminderHour)
        assertEquals(15, settings.reminderMinute)
        assertEquals(null, fakeRepository.scheduledHour)
    }

    @Test
    fun setBudgetAlertsEnabled_togglesPreference() = runTest {
        updateSettingsUseCase.setBudgetAlertsEnabled(false)
        assertFalse(getSettingsUseCase().first().isBudgetAlertsEnabled)

        updateSettingsUseCase.setBudgetAlertsEnabled(true)
        assertTrue(getSettingsUseCase().first().isBudgetAlertsEnabled)
    }

    @Test
    fun setGoalAlertsEnabled_togglesPreference() = runTest {
        updateSettingsUseCase.setGoalAlertsEnabled(false)
        assertFalse(getSettingsUseCase().first().isGoalAlertsEnabled)

        updateSettingsUseCase.setGoalAlertsEnabled(true)
        assertTrue(getSettingsUseCase().first().isGoalAlertsEnabled)
    }

    private class FakeNotificationRepository : NotificationRepository {
        private val _settingsFlow = MutableStateFlow(NotificationSettings())
        private val deliveredKeys = mutableSetOf<String>()

        var scheduledHour: Int? = null
        var scheduledMinute: Int? = null
        var isAlarmCancelled: Boolean = false

        override fun getNotificationSettings(): Flow<NotificationSettings> = _settingsFlow

        override suspend fun setDailyReminderEnabled(enabled: Boolean) {
            _settingsFlow.value = _settingsFlow.value.copy(isDailyReminderEnabled = enabled)
        }

        override suspend fun setReminderTime(hour: Int, minute: Int) {
            _settingsFlow.value = _settingsFlow.value.copy(reminderHour = hour, reminderMinute = minute)
        }

        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
            _settingsFlow.value = _settingsFlow.value.copy(isBudgetAlertsEnabled = enabled)
        }

        override suspend fun setGoalAlertsEnabled(enabled: Boolean) {
            _settingsFlow.value = _settingsFlow.value.copy(isGoalAlertsEnabled = enabled)
        }

        override suspend fun markAlertDelivered(key: String) {
            deliveredKeys.add(key)
        }

        override suspend fun isAlertDelivered(key: String): Boolean {
            return deliveredKeys.contains(key)
        }

        override fun scheduleDailyReminder(hour: Int, minute: Int) {
            scheduledHour = hour
            scheduledMinute = minute
            isAlarmCancelled = false
        }

        override fun cancelDailyReminder() {
            isAlarmCancelled = true
            scheduledHour = null
            scheduledMinute = null
        }
    }
}

package com.example.urwallet.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import com.example.urwallet.features.notifications.domain.usecase.GetNotificationSettingsUseCase
import com.example.urwallet.features.notifications.domain.usecase.UpdateNotificationSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase,
    private val updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _permissionGrantedFlow = MutableStateFlow(notificationHelper.hasNotificationPermission())
    private val _testMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        getNotificationSettingsUseCase(),
        _permissionGrantedFlow,
        _testMessageFlow
    ) { settings, isPermissionGranted, testMessage ->
        NotificationSettingsUiState(
            settings = settings,
            formattedReminderTime = formatArabicTime(settings.reminderHour, settings.reminderMinute),
            isPermissionGranted = isPermissionGranted,
            showPermissionBanner = !isPermissionGranted,
            testNotificationMessage = testMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationSettingsUiState(
            isPermissionGranted = notificationHelper.hasNotificationPermission(),
            showPermissionBanner = !notificationHelper.hasNotificationPermission()
        )
    )

    fun toggleDailyReminder(enabled: Boolean) {
        viewModelScope.launch {
            updateNotificationSettingsUseCase.setDailyReminderEnabled(enabled)
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            updateNotificationSettingsUseCase.setReminderTime(hour, minute)
        }
    }

    fun toggleBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            updateNotificationSettingsUseCase.setBudgetAlertsEnabled(enabled)
        }
    }

    fun toggleGoalAlerts(enabled: Boolean) {
        viewModelScope.launch {
            updateNotificationSettingsUseCase.setGoalAlertsEnabled(enabled)
        }
    }

    fun sendTestNotification() {
        notificationHelper.showTestNotification()
        _testMessageFlow.value = "تم إرسال الإشعار التجريبي 🔔"
    }

    fun clearTestMessage() {
        _testMessageFlow.value = null
    }

    fun updatePermissionStatus(isGranted: Boolean) {
        _permissionGrantedFlow.value = isGranted
    }

    private fun formatArabicTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
        return sdf.format(cal.time)
    }
}

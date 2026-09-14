package com.example.urwallet.features.notifications.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import com.example.urwallet.features.notifications.data.scheduler.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DailyReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context?, intent: Intent?) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isEnabled = appPreferences.isDailyReminderEnabled.first()
                if (isEnabled) {
                    notificationHelper.showDailyReminderNotification()

                    // Reschedule for the next day
                    val hour = appPreferences.reminderHour.first()
                    val minute = appPreferences.reminderMinute.first()
                    alarmScheduler.scheduleDailyReminder(hour, minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

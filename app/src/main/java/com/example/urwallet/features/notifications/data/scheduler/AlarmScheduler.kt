package com.example.urwallet.features.notifications.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.urwallet.features.notifications.data.receiver.DailyReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val ACTION_DAILY_REMINDER = "com.example.urwallet.ACTION_DAILY_REMINDER"
        const val REQUEST_CODE_DAILY_REMINDER = 5001

        /**
         * Pure function to calculate the next trigger time in epoch milliseconds.
         * If the target hour:minute has already passed today, schedules for tomorrow.
         */
        fun calculateNextTriggerTime(hour: Int, minute: Int, nowEpochMs: Long): Long {
            val nowCalendar = Calendar.getInstance().apply {
                timeInMillis = nowEpochMs
            }

            val targetCalendar = Calendar.getInstance().apply {
                timeInMillis = nowEpochMs
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (targetCalendar.timeInMillis <= nowCalendar.timeInMillis) {
                targetCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            return targetCalendar.timeInMillis
        }
    }

    private val alarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Schedules the next daily reminder using inexact idle alarm.
     * Reliable for reminders without demanding restricted exact-alarm privileges.
     */
    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val triggerTime = calculateNextTriggerTime(hour, minute, System.currentTimeMillis())
        val pendingIntent = getPendingIntent()

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (_: Exception) {
        }
    }

    /**
     * Cancels any scheduled daily reminder alarm.
     */
    fun cancelDailyReminder() {
        val pendingIntent = getPendingIntent()
        alarmManager.cancel(pendingIntent)
    }
}

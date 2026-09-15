package com.example.urwallet

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.features.more.presentation.recurring.RecurringTransactionWorker
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import com.example.urwallet.features.notifications.data.scheduler.AlarmScheduler
import com.example.urwallet.features.security.domain.session.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class UrWalletApplication : Application() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var appLockManager: AppLockManager

    override fun onCreate() {
        super.onCreate()
        appLockManager.syncLockStateBlocking()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockManager)
        notificationHelper.createNotificationChannels()
        scheduleRecurringTransactionsWorker()
        initializeDailyReminder()
    }

    private fun initializeDailyReminder() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isEnabled = appPreferences.isDailyReminderEnabled.first()
                if (isEnabled) {
                    val hour = appPreferences.reminderHour.first()
                    val minute = appPreferences.reminderMinute.first()
                    alarmScheduler.scheduleDailyReminder(hour, minute)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun scheduleRecurringTransactionsWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(this)

        workManager.enqueueUniquePeriodicWork(
            WORK_RECURRING_DAILY,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )

        // Trigger immediate check on startup for overdue items
        val oneTimeWork = OneTimeWorkRequestBuilder<RecurringTransactionWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            WORK_RECURRING_STARTUP_CHECK,
            ExistingWorkPolicy.KEEP,
            oneTimeWork
        )
    }

    companion object {
        const val WORK_RECURRING_DAILY = "urwallet_recurring_daily_worker"
        const val WORK_RECURRING_STARTUP_CHECK = "urwallet_recurring_startup_check"
    }
}

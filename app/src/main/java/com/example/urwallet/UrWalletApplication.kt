package com.example.urwallet

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.urwallet.features.more.presentation.recurring.RecurringTransactionWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class UrWalletApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleRecurringTransactionsWorker()
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

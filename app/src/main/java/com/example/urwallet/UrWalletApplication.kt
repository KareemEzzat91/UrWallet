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
import com.example.urwallet.features.more.data.worker.RecurringTransactionWorker
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import com.example.urwallet.features.notifications.data.scheduler.AlarmScheduler
import com.example.urwallet.features.security.domain.session.AppLockManager
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.common.Constants
import com.example.urwallet.core.common.SupportedCurrency
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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

    @Inject
    lateinit var appLockLifecycleObserver: com.example.urwallet.features.security.presentation.session.AppLockLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        initializeLocale()
        appLockManager.syncLockStateBlocking()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockLifecycleObserver)
        notificationHelper.createNotificationChannels()
        scheduleRecurringTransactionsWorker()
        scheduleFinancialEventScanWorker()
        initializeDailyReminder()
        initializeCurrency()
    }

    private fun initializeLocale() {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.isEmpty) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(com.example.urwallet.core.common.Constants.DEFAULT_LANGUAGE)
            )
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stored = appPreferences.appLanguage.first()
                val activeTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                if (activeTag.isNotEmpty() && activeTag != stored) {
                    appPreferences.setAppLanguage(activeTag)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun initializeCurrency() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                kotlinx.coroutines.flow.combine(
                    appPreferences.currencySymbol,
                    appPreferences.appLanguage
                ) { symbol, lang ->
                    val matched = Constants.SUPPORTED_CURRENCIES.find { it.symbol == symbol || it.code == symbol }
                    if (matched != null) {
                        matched.getSymbol(java.util.Locale(lang))
                    } else {
                        symbol
                    }
                }.collect { resolvedSymbol ->
                    Formatters.activeCurrencySymbol = resolvedSymbol
                }
            } catch (_: Exception) {
            }
        }
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

    private fun scheduleFinancialEventScanWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<com.example.urwallet.features.events.data.worker.FinancialEventScanWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_FINANCIAL_SCAN_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }

    companion object {
        const val WORK_RECURRING_DAILY = "urwallet_recurring_daily_worker"
        const val WORK_RECURRING_STARTUP_CHECK = "urwallet_recurring_startup_check"
        const val WORK_FINANCIAL_SCAN_PERIODIC = "urwallet_financial_scan_periodic_worker"
    }
}

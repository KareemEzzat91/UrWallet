package com.example.urwallet.features.events.data.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.urwallet.R
import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.features.events.domain.usecase.ScanRecentSmsUseCase
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class FinancialEventScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FinancialScanWorkerEntryPoint {
        fun scanRecentSmsUseCase(): ScanRecentSmsUseCase
        fun appPreferences(): AppPreferences
        fun notificationHelper(): NotificationHelper
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                FinancialScanWorkerEntryPoint::class.java
            )
            val appPreferences = entryPoint.appPreferences()
            val scanRecentSmsUseCase = entryPoint.scanRecentSmsUseCase()
            val notificationHelper = entryPoint.notificationHelper()

            // 1. Check if SMS detection is enabled
            val isEnabled = appPreferences.isSmsDetectionEnabled.first()
            if (!isEnabled) {
                return Result.success()
            }

            // 2. Check READ_SMS permission
            val hasPermission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                return Result.success()
            }

            // 3. Perform checkpoint-based incremental scan
            val newEventsCount = scanRecentSmsUseCase(forceFullScan = false)

            // 4. Trigger safe, idempotent notification if high-confidence events detected
            if (newEventsCount > 0) {
                val message = if (newEventsCount == 1) {
                    "تم اكتشاف معاملة مالية جديدة في الوارد المالي"
                } else {
                    "تم اكتشاف $newEventsCount معاملات مالية جديدة في الوارد المالي"
                }
                notificationHelper.sendFinancialEventsSummaryNotification(newEventsCount, message)
            }

            Result.success()
        } catch (e: Exception) {
            // Worker is retryable under transient failures
            Result.retry()
        }
    }
}

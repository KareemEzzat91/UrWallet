package com.example.urwallet.features.more.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.urwallet.features.more.domain.usecase.ProcessDueRecurringTransactionsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Background worker scheduled via WorkManager to run daily.
 * Strictly acts as an Android adapter delegating business logic to ProcessDueRecurringTransactionsUseCase.
 */
class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RecurringWorkerEntryPoint {
        fun processDueRecurringTransactionsUseCase(): ProcessDueRecurringTransactionsUseCase
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                RecurringWorkerEntryPoint::class.java
            )
            val useCase = entryPoint.processDueRecurringTransactionsUseCase()
            useCase()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

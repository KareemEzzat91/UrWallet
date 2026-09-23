package com.example.urwallet.features.events.domain.usecase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.urwallet.core.datastore.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ScanRecentSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processIncomingSmsUseCase: ProcessIncomingSmsUseCase,
    private val appPreferences: AppPreferences
) {

    suspend operator fun invoke(
        daysBack: Int = 14,
        forceFullScan: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val isEnabled = appPreferences.isSmsDetectionEnabled.first()
        if (!isEnabled) {
            return@withContext 0
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return@withContext 0
        }

        val lastCheckpoint = if (forceFullScan) 0L else appPreferences.lastSmsScanTimestamp.first()
        val defaultCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysBack.toLong())
        val cutoffTime = if (lastCheckpoint > 0L) lastCheckpoint else defaultCutoff

        var newEventsCount = 0
        var maxProcessedTimestamp = lastCheckpoint

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        // Checkpoint safety: use >= so messages with the exact same millisecond timestamp are never skipped.
        // Phase 17 sourceIdentifier idempotency prevents any duplicates.
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(cutoffTime.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        val scanResult = runCatching {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(Telephony.Sms._ID)
                val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getString(idIdx) else null
                    val sender = if (addrIdx != -1) cursor.getString(addrIdx).orEmpty() else ""
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx).orEmpty() else ""
                    val timestamp = if (dateIdx != -1) cursor.getLong(dateIdx) else System.currentTimeMillis()

                    if (body.isNotBlank()) {
                        val event = processIncomingSmsUseCase(
                            sender = sender,
                            message = body,
                            timestamp = timestamp,
                            originalSmsId = id
                        )
                        if (event != null) {
                            newEventsCount++
                        }
                    }

                    if (timestamp > maxProcessedTimestamp) {
                        maxProcessedTimestamp = timestamp
                    }
                }
            }
        }

        // Only advance the checkpoint if scan finished cleanly and new messages were processed
        if (scanResult.isSuccess && maxProcessedTimestamp > lastCheckpoint) {
            appPreferences.setLastSmsScanTimestamp(maxProcessedTimestamp)
        }

        return@withContext newEventsCount
    }
}

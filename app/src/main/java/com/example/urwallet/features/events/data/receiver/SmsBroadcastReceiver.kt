package com.example.urwallet.features.events.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.urwallet.R
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.features.events.domain.model.EventConfidence
import com.example.urwallet.features.events.domain.usecase.ProcessIncomingSmsUseCase
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var processIncomingSmsUseCase: ProcessIncomingSmsUseCase

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isDetectionEnabled = appPreferences.isSmsDetectionEnabled.first()
                if (!isDetectionEnabled) return@launch

                // Group multi-part SMS by originating address
                val groupedBySender = messages.groupBy { it.originatingAddress.orEmpty() }

                for ((sender, parts) in groupedBySender) {
                    val fullBody = parts.joinToString("") { it.messageBody.orEmpty() }
                    val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                    if (fullBody.isNotBlank()) {
                        val event = processIncomingSmsUseCase(
                            sender = sender,
                            message = fullBody,
                            timestamp = timestamp
                        )

                        // If high-confidence financial event detected and successfully inserted, dispatch local notification
                        if (event != null && event.id > 0L && event.confidence == EventConfidence.HIGH) {
                            val actionTypeStr = if (event.type == TransactionType.INCOME) "إيداع" else "خصم"
                            val amountStr = Formatters.formatCurrency(event.amount)
                            val title = context.getString(R.string.notification_financial_event_title)
                            val message = context.getString(
                                R.string.notification_financial_event_desc,
                                actionTypeStr,
                                amountStr,
                                event.sender
                            )
                            notificationHelper.sendFinancialEventNotification(event.id, title, message)
                        }
                    }
                }
            } catch (e: Exception) {
                // Never crash on receiver errors
            } finally {
                pendingResult.finish()
            }
        }
    }
}

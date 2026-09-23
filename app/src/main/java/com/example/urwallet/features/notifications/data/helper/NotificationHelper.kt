package com.example.urwallet.features.notifications.data.helper

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.urwallet.MainActivity
import com.example.urwallet.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class NotificationHelper {

    protected val context: Context?

    @Inject
    constructor(@ApplicationContext context: Context) {
        this.context = context
    }

    // Secondary parameterless constructor for test fakes
    constructor() {
        this.context = null
    }

    companion object {
        const val CHANNEL_DAILY_REMINDER = "urwallet_channel_daily_reminder"
        const val CHANNEL_BUDGET_ALERTS = "urwallet_channel_budget_alerts"
        const val CHANNEL_GOAL_MILESTONES = "urwallet_channel_goal_milestones"
        const val CHANNEL_CHALLENGES = "urwallet_channel_challenges"

        const val EXTRA_NAV_TARGET = "extra_nav_target"
        const val NAV_TARGET_ADD_TRANSACTION = "add_transaction"
        const val NAV_TARGET_BUDGETS = "budgets"
        const val NAV_TARGET_GOALS = "goals"
        const val NAV_TARGET_SETTINGS = "notification_settings"
        const val NAV_TARGET_FINANCIAL_INBOX = "financial_inbox"

        const val CHANNEL_FINANCIAL_INBOX = "urwallet_channel_financial_inbox"

        const val ID_DAILY_REMINDER = 1001
        const val ID_BUDGET_ALERT_GLOBAL = 2000
        const val ID_BUDGET_ALERT_BASE = 2100
        const val ID_GOAL_MILESTONE_BASE = 3000
        const val ID_FINANCIAL_INBOX_BASE = 4000
        const val ID_TEST_NOTIFICATION = 9999
    }

    /**
     * Initializes all 5 system notification channels with appropriate importance,
     * Arabic labels, and descriptions. Safe to call repeatedly.
     */
    open fun createNotificationChannels() {
        val ctx = context ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_DAILY_REMINDER,
                    ctx.getString(R.string.notification_channel_daily_reminder_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = ctx.getString(R.string.notification_channel_daily_reminder_desc)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_BUDGET_ALERTS,
                    ctx.getString(R.string.notification_channel_budgets_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = ctx.getString(R.string.notification_channel_budgets_desc)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_GOAL_MILESTONES,
                    ctx.getString(R.string.notification_channel_goals_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = ctx.getString(R.string.notification_channel_goals_desc)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_CHALLENGES,
                    ctx.getString(R.string.notification_channel_challenges_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = ctx.getString(R.string.notification_channel_challenges_desc)
                },
                NotificationChannel(
                    CHANNEL_FINANCIAL_INBOX,
                    ctx.getString(R.string.notification_channel_financial_inbox_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = ctx.getString(R.string.notification_channel_financial_inbox_desc)
                    enableVibration(true)
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Checks if the app is currently granted POST_NOTIFICATIONS permission.
     */
    open fun hasNotificationPermission(): Boolean {
        val ctx = context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Dispatches daily reminder notification prompting the user to log transactions.
     * Tapping deep links directly to AddTransactionBottomSheet.
     */
    @SuppressLint("MissingPermission")
    open fun showDailyReminderNotification() {
        val ctx = context ?: return
        if (!hasNotificationPermission()) return

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_ADD_TRANSACTION)
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            ID_DAILY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_bell)
            .setColor(ContextCompat.getColor(ctx, R.color.urwallet_primary))
            .setContentTitle(ctx.getString(R.string.daily_reminder_title))
            .setContentText(ctx.getString(R.string.daily_reminder_message))
            .setStyle(NotificationCompat.BigTextStyle().bigText(ctx.getString(R.string.daily_reminder_message)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(ctx).notify(ID_DAILY_REMINDER, notification)
    }

    /**
     * Dispatches a budget threshold warning or exceeded notification.
     * Tapping deep links directly to BudgetsFragment.
     */
    @SuppressLint("MissingPermission")
    open fun showBudgetAlertNotification(
        budgetName: String,
        percentage: Int,
        isExceeded: Boolean,
        categoryId: Long? = null
    ) {
        val ctx = context ?: return
        if (!hasNotificationPermission()) return

        val title = if (isExceeded) {
            ctx.getString(R.string.budget_alert_exceeded_title)
        } else {
            ctx.getString(R.string.budget_alert_warning_title)
        }

        val message = if (isExceeded) {
            ctx.getString(R.string.budget_alert_exceeded_desc, budgetName)
        } else {
            ctx.getString(R.string.budget_alert_warning_desc, percentage, budgetName)
        }

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_BUDGETS)
        }

        val notificationId = if (categoryId != null) {
            ID_BUDGET_ALERT_BASE + (categoryId % 1000).toInt()
        } else {
            ID_BUDGET_ALERT_GLOBAL
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_bell)
            .setColor(ContextCompat.getColor(ctx, R.color.urwallet_expense))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(ctx).notify(notificationId, notification)
    }

    /**
     * Dispatches a celebratory goal milestone notification (50% or 100%).
     * Tapping deep links directly to GoalsFragment.
     */
    @SuppressLint("MissingPermission")
    open fun showGoalMilestoneNotification(
        goalName: String,
        milestonePercentage: Int,
        isCompleted: Boolean,
        goalId: Long = 0L
    ) {
        val ctx = context ?: return
        if (!hasNotificationPermission()) return

        val title = if (isCompleted) {
            ctx.getString(R.string.goal_milestone_completed_title)
        } else {
            ctx.getString(R.string.goal_milestone_half_title)
        }

        val message = if (isCompleted) {
            ctx.getString(R.string.goal_milestone_completed_desc, goalName)
        } else {
            ctx.getString(R.string.goal_milestone_half_desc, goalName)
        }

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_GOALS)
        }

        val notificationId = ID_GOAL_MILESTONE_BASE + (goalId % 1000).toInt()

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_GOAL_MILESTONES)
            .setSmallIcon(R.drawable.ic_bell)
            .setColor(ContextCompat.getColor(ctx, R.color.urwallet_primary))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(ctx).notify(notificationId, notification)
    }

    /**
     * Dispatches an immediate test notification for user verification.
     */
    @SuppressLint("MissingPermission")
    open fun showTestNotification() {
        val ctx = context ?: return
        if (!hasNotificationPermission()) return

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_SETTINGS)
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            ID_TEST_NOTIFICATION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_bell)
            .setColor(ContextCompat.getColor(ctx, R.color.urwallet_primary))
            .setContentTitle(ctx.getString(R.string.test_notification_title))
            .setContentText(ctx.getString(R.string.test_notification_desc))
            .setStyle(NotificationCompat.BigTextStyle().bigText(ctx.getString(R.string.test_notification_desc)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(ctx).notify(ID_TEST_NOTIFICATION, notification)
    }

    /**
     * Dispatches notification for newly detected high-confidence financial events.
     */
    @SuppressLint("MissingPermission")
    open fun sendFinancialEventNotification(eventId: Long, title: String, message: String) {
        val ctx = context ?: return
        if (!hasNotificationPermission()) return

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_FINANCIAL_INBOX)
            putExtra("eventId", eventId)
        }

        val notificationId = (ID_FINANCIAL_INBOX_BASE + (eventId % 1000)).toInt()

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_FINANCIAL_INBOX)
            .setSmallIcon(R.drawable.ic_nav_transactions)
            .setColor(ContextCompat.getColor(ctx, R.color.urwallet_primary))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(ctx).notify(notificationId, notification)
    }

    /**
     * Dispatches a batched summary notification when multiple financial events are detected in the background.
     */
    @SuppressLint("MissingPermission")
    open fun sendFinancialEventsSummaryNotification(count: Int, message: String) {
        val ctx = context ?: return
        if (!hasNotificationPermission()) return

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_FINANCIAL_INBOX)
        }

        val notificationId = ID_FINANCIAL_INBOX_BASE

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = ctx.getString(R.string.title_financial_inbox)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_FINANCIAL_INBOX)
            .setSmallIcon(R.drawable.ic_nav_transactions)
            .setColor(ContextCompat.getColor(ctx, R.color.urwallet_primary))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(ctx).notify(notificationId, notification)
    }
}

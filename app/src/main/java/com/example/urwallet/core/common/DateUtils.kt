package com.example.urwallet.core.common

import com.example.urwallet.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    fun getCurrentEpochMs(): Long = System.currentTimeMillis()

    fun getStartOfDay(epochMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfDay(epochMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun getStartOfMonth(month: Int, year: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfMonth(month: Int, year: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1
    }

    const val LABEL_TODAY = "TODAY"
    const val LABEL_YESTERDAY = "YESTERDAY"

    fun getCurrentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun getDateGroupKey(epochMs: Long): String {
        val todayStart = getStartOfDay()
        val yesterdayStart = todayStart - (24 * 60 * 60 * 1000)
        return when {
            epochMs >= todayStart -> LABEL_TODAY
            epochMs >= yesterdayStart -> LABEL_YESTERDAY
            else -> formatDisplayDate(epochMs)
        }
    }

    fun formatDateLocalized(
        epochMs: Long,
        context: android.content.Context,
        locale: Locale = Locale.getDefault()
    ): String {
        val todayStart = getStartOfDay()
        val yesterdayStart = todayStart - (24 * 60 * 60 * 1000)

        return when {
            epochMs >= todayStart -> context.getString(R.string.date_today)
            epochMs >= yesterdayStart -> context.getString(R.string.date_yesterday)
            else -> {
                val formatter = SimpleDateFormat("dd MMMM yyyy", locale)
                formatter.format(Date(epochMs))
            }
        }
    }

    fun formatDisplayDate(epochMs: Long, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat("dd MMMM yyyy", locale)
        return formatter.format(Date(epochMs))
    }

    fun formatTime(epochMs: Long, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat("hh:mm a", locale)
        return formatter.format(Date(epochMs))
    }

    fun formatMonthYearLocalized(month: Int, year: Int, locale: Locale = Locale.getDefault()): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.YEAR, year)
        }
        val formatter = SimpleDateFormat("MMMM yyyy", locale)
        return formatter.format(calendar.time)
    }

    fun formatMonthName(month: Int, locale: Locale = Locale.getDefault()): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val formatter = SimpleDateFormat("MMMM", locale)
        return formatter.format(calendar.time)
    }

    fun getStartOfWeek(epochMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMs
            firstDayOfWeek = Calendar.SATURDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis > epochMs) {
            calendar.add(Calendar.DAY_OF_MONTH, -7)
        }
        return calendar.timeInMillis
    }

    fun getEndOfWeek(epochMs: Long = System.currentTimeMillis()): Long {
        val startOfWeek = getStartOfWeek(epochMs)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startOfWeek
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun getPreviousMonth(month: Int, year: Int): Pair<Int, Int> {
        return if (month <= 1) {
            Pair(12, year - 1)
        } else {
            Pair(month - 1, year)
        }
    }

    fun getNextMonth(month: Int, year: Int): Pair<Int, Int> {
        return if (month >= 12) {
            Pair(1, year + 1)
        } else {
            Pair(month + 1, year)
        }
    }

    fun getDaysInMonth(month: Int, year: Int): Int {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getDayOfMonth(epochMs: Long): Int {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMs
        }
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    fun getDayOfWeek(epochMs: Long): Int {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMs
        }
        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    fun getCurrentDayOfMonth(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    fun getPeriodDateRange(
        period: com.example.urwallet.features.analytics.domain.model.AnalyticsTimePeriod,
        referenceMonth: Int = getCurrentMonth(),
        referenceYear: Int = getCurrentYear()
    ): Pair<Long, Long> {
        return when (period) {
            com.example.urwallet.features.analytics.domain.model.AnalyticsTimePeriod.THIS_MONTH -> {
                Pair(getStartOfMonth(referenceMonth, referenceYear), getEndOfMonth(referenceMonth, referenceYear))
            }
            com.example.urwallet.features.analytics.domain.model.AnalyticsTimePeriod.LAST_MONTH -> {
                val (prevM, prevY) = getPreviousMonth(referenceMonth, referenceYear)
                Pair(getStartOfMonth(prevM, prevY), getEndOfMonth(prevM, prevY))
            }
            com.example.urwallet.features.analytics.domain.model.AnalyticsTimePeriod.LAST_3_MONTHS -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, referenceYear)
                    set(Calendar.MONTH, referenceMonth - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -2)
                }
                val startM = cal.get(Calendar.MONTH) + 1
                val startY = cal.get(Calendar.YEAR)
                Pair(getStartOfMonth(startM, startY), getEndOfMonth(referenceMonth, referenceYear))
            }
            com.example.urwallet.features.analytics.domain.model.AnalyticsTimePeriod.LAST_6_MONTHS -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, referenceYear)
                    set(Calendar.MONTH, referenceMonth - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -5)
                }
                val startM = cal.get(Calendar.MONTH) + 1
                val startY = cal.get(Calendar.YEAR)
                Pair(getStartOfMonth(startM, startY), getEndOfMonth(referenceMonth, referenceYear))
            }
            com.example.urwallet.features.analytics.domain.model.AnalyticsTimePeriod.THIS_YEAR -> {
                Pair(getStartOfMonth(1, referenceYear), getEndOfMonth(12, referenceYear))
            }
        }
    }
}

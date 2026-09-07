package com.example.urwallet.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val ARABIC_LOCALE = Locale("ar")

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

    fun getCurrentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun formatDateArabic(epochMs: Long): String {
        val todayStart = getStartOfDay()
        val yesterdayStart = todayStart - (24 * 60 * 60 * 1000)

        return when {
            epochMs >= todayStart -> "اليوم"
            epochMs >= yesterdayStart -> "أمس"
            else -> {
                val formatter = SimpleDateFormat("dd MMMM yyyy", ARABIC_LOCALE)
                formatter.format(Date(epochMs))
            }
        }
    }

    fun formatDisplayDate(epochMs: Long): String {
        val formatter = SimpleDateFormat("dd MMMM yyyy", ARABIC_LOCALE)
        return formatter.format(Date(epochMs))
    }

    fun formatTimeArabic(epochMs: Long): String {
        val formatter = SimpleDateFormat("hh:mm a", ARABIC_LOCALE)
        return formatter.format(Date(epochMs))
    }

    fun formatTime(epochMs: Long): String = formatTimeArabic(epochMs)

    fun formatMonthYearArabic(month: Int, year: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.YEAR, year)
        }
        val formatter = SimpleDateFormat("MMMM yyyy", ARABIC_LOCALE)
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
}

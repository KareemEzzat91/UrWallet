package com.example.urwallet.features.notifications

import com.example.urwallet.features.notifications.data.scheduler.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AlarmSchedulerTest {

    @Test
    fun calculateNextTriggerTime_futureTimeToday_returnsTodayWithTargetHourMinute() {
        // Base: Today at 10:00:00
        val baseCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = baseCalendar.timeInMillis

        // Target: Today at 21:00
        val resultMs = AlarmScheduler.calculateNextTriggerTime(hour = 21, minute = 0, nowEpochMs = nowMs)

        val resultCal = Calendar.getInstance().apply { timeInMillis = resultMs }
        assertEquals(baseCalendar.get(Calendar.DAY_OF_YEAR), resultCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(21, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun calculateNextTriggerTime_pastTimeToday_returnsTomorrowWithTargetHourMinute() {
        // Base: Today at 22:30:00
        val baseCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = baseCalendar.timeInMillis

        // Target: 21:00 (already passed)
        val resultMs = AlarmScheduler.calculateNextTriggerTime(hour = 21, minute = 0, nowEpochMs = nowMs)

        val resultCal = Calendar.getInstance().apply { timeInMillis = resultMs }
        assertTrue(resultMs > nowMs)
        val expectedCal = (baseCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(expectedCal.get(Calendar.DAY_OF_YEAR), resultCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(21, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun calculateNextTriggerTime_exactSameTime_returnsTomorrow() {
        // Base: Exactly 21:00:00.000
        val baseCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = baseCalendar.timeInMillis

        val resultMs = AlarmScheduler.calculateNextTriggerTime(hour = 21, minute = 0, nowEpochMs = nowMs)

        val resultCal = Calendar.getInstance().apply { timeInMillis = resultMs }
        assertTrue(resultMs > nowMs)
        val expectedCal = (baseCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(expectedCal.get(Calendar.DAY_OF_YEAR), resultCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(21, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun calculateNextTriggerTime_midnightBoundary_handlesCorrectly() {
        // Base: Today at 23:55
        val baseCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 55)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = baseCalendar.timeInMillis

        // Target: Midnight (00:00) -> should be scheduled for tomorrow morning 00:00
        val resultMs = AlarmScheduler.calculateNextTriggerTime(hour = 0, minute = 0, nowEpochMs = nowMs)

        val resultCal = Calendar.getInstance().apply { timeInMillis = resultMs }
        assertTrue(resultMs > nowMs)
        val expectedCal = (baseCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(expectedCal.get(Calendar.DAY_OF_YEAR), resultCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun calculateNextTriggerTime_endOfDayBoundary_handlesCorrectly() {
        // Base: Today at 12:00
        val baseCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = baseCalendar.timeInMillis

        // Target: 23:59
        val resultMs = AlarmScheduler.calculateNextTriggerTime(hour = 23, minute = 59, nowEpochMs = nowMs)

        val resultCal = Calendar.getInstance().apply { timeInMillis = resultMs }
        assertEquals(baseCalendar.get(Calendar.DAY_OF_YEAR), resultCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(23, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, resultCal.get(Calendar.MINUTE))
    }
}

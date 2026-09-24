package com.example.urwallet.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DateUtilsLocalizedTest {

    @Test
    fun `formatMonthName in English returns correct English month names`() {
        val january = DateUtils.formatMonthName(1, Locale.ENGLISH)
        val june = DateUtils.formatMonthName(6, Locale.ENGLISH)
        val december = DateUtils.formatMonthName(12, Locale.ENGLISH)

        assertEquals("January", january)
        assertEquals("June", june)
        assertEquals("December", december)
    }

    @Test
    fun `formatMonthName in Arabic returns correct Arabic month names`() {
        val arabicLocale = Locale("ar")
        val january = DateUtils.formatMonthName(1, arabicLocale)
        val december = DateUtils.formatMonthName(12, arabicLocale)

        assertEquals("يناير", january)
        assertEquals("ديسمبر", december)
    }

    @Test
    fun `formatMonthYearLocalized in English returns formatted string`() {
        val result = DateUtils.formatMonthYearLocalized(9, 2026, Locale.ENGLISH)
        assertEquals("September 2026", result)
    }

    @Test
    fun `formatMonthYearLocalized in Arabic returns localized string`() {
        val result = DateUtils.formatMonthYearLocalized(9, 2026, Locale("ar"))
        assertTrue(result.contains("سبتمبر"))
        assertTrue(result.contains("2026") || result.contains("٢٠٢٦"))
    }

    @Test
    fun `getDateGroupKey correctly categorizes today and yesterday`() {
        val now = System.currentTimeMillis()
        val todayKey = DateUtils.getDateGroupKey(now)
        assertEquals(DateUtils.LABEL_TODAY, todayKey)

        val yesterdayEpoch = DateUtils.getStartOfDay(now) - 3600_000L // 1 hour before start of today
        val yesterdayKey = DateUtils.getDateGroupKey(yesterdayEpoch)
        assertEquals(DateUtils.LABEL_YESTERDAY, yesterdayKey)
    }

    @Test
    fun `supportedCurrency getDisplayName returns localized names`() {
        val egp = Constants.SUPPORTED_CURRENCIES.first { it.code == "EGP" }
        assertEquals("Egyptian Pound (EGP)", egp.getDisplayName("en"))
        assertEquals("جنيه مصري (ج.م)", egp.getDisplayName("ar"))

        val usd = Constants.SUPPORTED_CURRENCIES.first { it.code == "USD" }
        assertEquals("US Dollar ($)", usd.getDisplayName("en"))
        assertEquals("دولار أمريكي ($)", usd.getDisplayName("ar"))
    }
}

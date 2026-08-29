package com.example.urwallet.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `start of month should be first day at 00 00`() {
        val startEpoch = DateUtils.getStartOfMonth(month = 8, year = 2026)
        val endEpoch = DateUtils.getEndOfMonth(month = 8, year = 2026)

        assertTrue(startEpoch < endEpoch)
    }

    @Test
    fun `currency format should include symbol`() {
        val formatted = Formatters.formatCurrency(1500.0, "ج.م", includeDecimals = false)
        assertEquals("1,500 ج.م", formatted)
    }

    @Test
    fun `signed amount for expense should have minus sign`() {
        val formatted = Formatters.formatSignedAmount(250.0, TransactionType.EXPENSE, "ج.م")
        assertEquals("- 250 ج.م", formatted)
    }

    @Test
    fun `signed amount for income should have plus sign`() {
        val formatted = Formatters.formatSignedAmount(5000.0, TransactionType.INCOME, "ج.م")
        assertEquals("+ 5,000 ج.م", formatted)
    }
}

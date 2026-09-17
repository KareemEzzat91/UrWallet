package com.example.urwallet.core.common

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormattersTest {

    @Before
    fun setUp() {
        Formatters.activeCurrencySymbol = Constants.DEFAULT_CURRENCY_SYMBOL
    }

    @After
    fun tearDown() {
        Formatters.activeCurrencySymbol = Constants.DEFAULT_CURRENCY_SYMBOL
    }

    @Test
    fun `formatCurrency with default currency symbol formats properly`() {
        val result = Formatters.formatCurrency(1500.0)
        assertEquals("1,500 ج.م", result)
    }

    @Test
    fun `formatCurrency with decimal amount includes two decimals`() {
        val result = Formatters.formatCurrency(1500.5)
        assertEquals("1,500.50 ج.م", result)
    }

    @Test
    fun `formatCurrency with explicit currency symbol overrides default`() {
        val result = Formatters.formatCurrency(250.0, currencySymbol = "$")
        assertEquals("250 $", result)
    }

    @Test
    fun `formatCurrency dynamically reflects activeCurrencySymbol change`() {
        Formatters.activeCurrencySymbol = "ر.س"
        val result = Formatters.formatCurrency(500.0)
        assertEquals("500 ر.س", result)
    }

    @Test
    fun `formatSignedAmount with active currency formats income and expense correctly`() {
        Formatters.activeCurrencySymbol = "د.إ"

        val incomeResult = Formatters.formatSignedAmount(1000.0, TransactionType.INCOME)
        assertEquals("+ 1,000 د.إ", incomeResult)

        val expenseResult = Formatters.formatSignedAmount(200.0, TransactionType.EXPENSE)
        assertEquals("- 200 د.إ", expenseResult)
    }

    @Test
    fun `formatPercentage formats whole and decimal numbers correctly`() {
        val whole = Formatters.formatPercentage(75.0)
        assertEquals("75%", whole)

        val decimal = Formatters.formatPercentage(33.33)
        assertEquals("33.3%", decimal)
    }

    @Test
    fun `supported currencies list contains required primary currencies`() {
        val symbols = Constants.SUPPORTED_CURRENCIES.map { it.symbol }
        assertTrue(symbols.contains("ج.م"))
        assertTrue(symbols.contains("ر.س"))
        assertTrue(symbols.contains("د.إ"))
        assertTrue(symbols.contains("$"))
        assertTrue(symbols.contains("€"))
    }
}

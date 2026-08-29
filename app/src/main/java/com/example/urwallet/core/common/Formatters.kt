package com.example.urwallet.core.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Formatters {

    private val ARABIC_LOCALE = Locale("ar")

    fun formatCurrency(
        amount: Double,
        currencySymbol: String = Constants.DEFAULT_CURRENCY_SYMBOL,
        includeDecimals: Boolean = true
    ): String {
        val pattern = if (includeDecimals && amount % 1.0 != 0.0) "#,##0.00" else "#,##0"
        val symbols = DecimalFormatSymbols(Locale.US) // Keep numerals clean
        val formatter = DecimalFormat(pattern, symbols)
        return "${formatter.format(amount)} $currencySymbol"
    }

    fun formatPercentage(percentage: Double): String {
        val formatted = if (percentage % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", percentage)
        } else {
            String.format(Locale.US, "%.1f", percentage)
        }
        return "$formatted%"
    }

    fun formatSignedAmount(
        amount: Double,
        type: TransactionType,
        currencySymbol: String = Constants.DEFAULT_CURRENCY_SYMBOL
    ): String {
        val formatted = formatCurrency(amount, currencySymbol)
        return when (type) {
            TransactionType.INCOME -> "+ $formatted"
            TransactionType.EXPENSE -> "- $formatted"
        }
    }
}

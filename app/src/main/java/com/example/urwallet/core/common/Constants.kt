package com.example.urwallet.core.common

object Constants {
    const val DATABASE_NAME = "urwallet.db"
    const val PREFERENCES_NAME = "urwallet_preferences"

    // Default Currency
    const val DEFAULT_CURRENCY_CODE = "EGP"
    const val DEFAULT_CURRENCY_SYMBOL = "ج.م"

    // Budget defaults
    const val DEFAULT_ALERT_THRESHOLD = 0.80 // 80%

    // Schema version for backup file
    const val BACKUP_SCHEMA_VERSION = 1
    const val APP_VERSION = "1.0.0"

    val SUPPORTED_CURRENCIES = listOf(
        SupportedCurrency("EGP", "ج.م", "جنيه مصري (ج.م)"),
        SupportedCurrency("SAR", "ر.س", "ريال سعودي (ر.س)"),
        SupportedCurrency("AED", "د.إ", "درهم إماراتي (د.إ)"),
        SupportedCurrency("KWD", "د.ك", "دينار كويتي (د.ك)"),
        SupportedCurrency("QAR", "ر.ق", "ريال قطري (ر.ق)"),
        SupportedCurrency("OMR", "ر.ع", "ريال عماني (ر.ع)"),
        SupportedCurrency("BHD", "د.ب", "دينار بحريني (د.ب)"),
        SupportedCurrency("JOD", "د.أ", "دينار أردني (د.أ)"),
        SupportedCurrency("USD", "$", "دولار أمريكي ($)"),
        SupportedCurrency("EUR", "€", "يورو (€)"),
        SupportedCurrency("GBP", "£", "جنيه إسترليني (£)")
    )
}

data class SupportedCurrency(
    val code: String,
    val symbol: String,
    val displayNameAr: String
)

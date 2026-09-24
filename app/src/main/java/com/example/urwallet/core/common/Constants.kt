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

    // Supported Languages
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_ARABIC = "ar"
    const val DEFAULT_LANGUAGE = LANGUAGE_ENGLISH

    val SUPPORTED_CURRENCIES = listOf(
        SupportedCurrency("EGP", "ج.م", "جنيه مصري (ج.م)", "Egyptian Pound (EGP)"),
        SupportedCurrency("SAR", "ر.س", "ريال سعودي (ر.س)", "Saudi Riyal (SAR)"),
        SupportedCurrency("AED", "د.إ", "درهم إماراتي (د.إ)", "UAE Dirham (AED)"),
        SupportedCurrency("KWD", "د.ك", "دينار كويتي (د.ك)", "Kuwaiti Dinar (KWD)"),
        SupportedCurrency("QAR", "ر.ق", "ريال قطري (ر.ق)", "Qatari Riyal (QAR)"),
        SupportedCurrency("OMR", "ر.ع", "ريال عماني (ر.ع)", "Omani Rial (OMR)"),
        SupportedCurrency("BHD", "د.ب", "دينار بحريني (د.ب)", "Bahraini Dinar (BHD)"),
        SupportedCurrency("JOD", "د.أ", "دينار أردني (د.أ)", "Jordanian Dinar (JOD)"),
        SupportedCurrency("USD", "$", "دولار أمريكي ($)", "US Dollar ($)"),
        SupportedCurrency("EUR", "€", "يورو (€)", "Euro (€)"),
        SupportedCurrency("GBP", "£", "جنيه إسترليني (£)", "British Pound (£)")
    )
}

data class SupportedCurrency(
    val code: String,
    val symbol: String,
    val displayNameAr: String,
    val displayNameEn: String = "",
    val symbolEn: String = if (symbol in listOf("$", "€", "£")) symbol else code
) {
    fun getDisplayName(locale: java.util.Locale = java.util.Locale.getDefault()): String {
        return if (locale.language.startsWith("ar")) displayNameAr else displayNameEn.ifEmpty { displayNameAr }
    }

    fun getDisplayName(languageCode: String): String {
        return getDisplayName(java.util.Locale(languageCode))
    }

    fun getSymbol(locale: java.util.Locale = java.util.Locale.getDefault()): String {
        return if (locale.language.startsWith("ar")) symbol else symbolEn
    }
}

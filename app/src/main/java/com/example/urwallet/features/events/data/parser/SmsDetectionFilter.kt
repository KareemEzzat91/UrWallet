package com.example.urwallet.features.events.data.parser

import java.util.Locale

object SmsDetectionFilter {

    private val OTP_PATTERNS = listOf(
        "otp",
        "رمز التحقق",
        "كود التأكيد",
        "كود التفعيل",
        "رمز الأمان",
        "one time password",
        "verification code",
        "security code",
        "authentication code",
        "secret code",
        "لا تشارك هذا الرمز",
        "do not share",
        "never share"
    )

    private val BALANCE_ONLY_PATTERNS = listOf(
        "رصيدك الحالي هو",
        "رصيدك هو",
        "رصيد حسابك هو",
        "available balance is",
        "current balance is",
        "your balance is",
        "معرفة رصيدك"
    )

    private val MARKETING_PATTERNS = listOf(
        "خصم يصل إلى",
        "اشترك الآن",
        "عروض حصرية",
        "عرض خاص",
        "كاش باك حتى",
        "discount up to",
        "special offer",
        "apply now",
        "win with",
        "مبروك كسبت"
    )

    private val FINANCIAL_INDICATORS = listOf(
        // Arabic keywords
        "خصم",
        "سحب",
        "إيداع",
        "ايداع",
        "تحويل",
        "شراء",
        "استلام",
        "مدفوعات",
        "سداد",
        "حوالة",
        "دفع فواتير",
        "قسط",
        "مصاريف",
        "تم خصم",
        "تم سحب",
        "تم تحويل",
        "تم استلام",
        "إضافة",
        "اضافة",
        "تم إضافة",
        // English keywords
        "debited",
        "credited",
        "purchase",
        "withdrawn",
        "transferred",
        "received",
        "payment",
        "deposited",
        "cash withdrawal",
        "refund"
    )

    /**
     * Determines whether the SMS represents a financial transaction event.
     * Rejects OTPs, balance-only queries, and promotional marketing messages.
     */
    fun isPotentialFinancialSms(message: String): Boolean {
        val lower = message.lowercase(Locale.ROOT)

        // 1. Must NOT be an OTP or verification message
        if (OTP_PATTERNS.any { lower.contains(it) }) {
            return false
        }

        // 2. Must NOT be pure marketing
        if (MARKETING_PATTERNS.any { lower.contains(it) }) {
            return false
        }

        // 3. Must NOT be balance inquiry only (unless combined with actual transaction verbs)
        if (BALANCE_ONLY_PATTERNS.any { lower.contains(it) }) {
            val hasActionVerb = listOf("خصم", "سحب", "تحويل", "شراء", "debited", "credited", "purchase").any { lower.contains(it) }
            if (!hasActionVerb) return false
        }

        // 4. Must contain at least one financial action verb or indicator
        val hasFinancialIndicator = FINANCIAL_INDICATORS.any { lower.contains(it) }
        if (!hasFinancialIndicator) return false

        // 5. Must contain at least one number (amount)
        val hasDigits = message.any { it.isDigit() || ArabicNumberNormalizer.normalizeDigits(it.toString()).any { d -> d.isDigit() } }
        return hasDigits
    }
}

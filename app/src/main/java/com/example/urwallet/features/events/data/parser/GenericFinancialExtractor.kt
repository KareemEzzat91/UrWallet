package com.example.urwallet.features.events.data.parser

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.EventConfidence
import com.example.urwallet.features.events.domain.model.FinancialEventSource
import com.example.urwallet.features.events.domain.model.PartialFinancialEvent
import java.util.Locale
import java.util.regex.Pattern

class GenericFinancialExtractor : FinancialEventExtractor {

    override val name: String = "GenericEgyptianBank"

    // Amount patterns with Egyptian Pound indicators
    private val AMOUNT_WITH_CURRENCY_AR = Pattern.compile("([\\d.,٠-٩]+)\\s*(?:ج\\.م|جنيه|جم)", Pattern.CASE_INSENSITIVE)
    private val AMOUNT_WITH_CURRENCY_EN = Pattern.compile("(?:EGP|LE)\\s*([\\d.,]+)|([\\d.,]+)\\s*(?:EGP|LE)", Pattern.CASE_INSENSITIVE)
    private val MASKED_CARD_REGEX = Pattern.compile("(?:بطاقة|حساب|كارت|card|account)?\\s*[*]+([\\d]{4})", Pattern.CASE_INSENSITIVE)

    private val EXPENSE_VERBS = listOf(
        "تم خصم",
        "خصم",
        "تم سحب",
        "سحب نقدي",
        "سحب",
        "شراء",
        "سداد",
        "مدفوعات",
        "تحويل إلى",
        "debited",
        "withdrawn",
        "purchase",
        "paid",
        "transferred to"
    )

    private val INCOME_VERBS = listOf(
        "تم إيداع",
        "إيداع",
        "ايداع",
        "تم استلام",
        "استلام",
        "تم إضافة",
        "تمت إضافة",
        "إضافة",
        "اضافة",
        "تحويل من",
        "حوالة واردة",
        "أضيف لحسابك",
        "مرتب",
        "credited",
        "deposited",
        "received",
        "transferred from"
    )

    override fun canHandle(sender: String, message: String): Boolean {
        // Fallback extractor can handle any message that passed the initial detection filter
        return SmsDetectionFilter.isPotentialFinancialSms(message)
    }

    override fun extract(sender: String, message: String, timestamp: Long, originalSmsId: String?): PartialFinancialEvent? {
        val lower = message.lowercase(Locale.ROOT)
        val normalizedMsg = ArabicNumberNormalizer.normalizeDigits(message)

        // 1. Determine direction / type
        val isExpense = EXPENSE_VERBS.any { lower.contains(it) }
        val isIncome = INCOME_VERBS.any { lower.contains(it) }

        if (!isExpense && !isIncome) {
            // Cannot reliably determine direction
            return null
        }

        // If message contains both, prioritize expense if "خصم" or "شراء" is prominent, else income
        val type = if (isExpense && !isIncome) {
            TransactionType.EXPENSE
        } else if (isIncome && !isExpense) {
            TransactionType.INCOME
        } else {
            // Both matched (e.g. "تم تحويل ... واستلام ...")
            if (lower.contains("تم خصم") || lower.contains("debited") || lower.contains("سحب")) {
                TransactionType.EXPENSE
            } else {
                TransactionType.INCOME
            }
        }

        // 2. Extract amount
        var amount: Double? = null
        var hasExplicitCurrency = false

        // Try Arabic amount with currency
        val arMatcher = AMOUNT_WITH_CURRENCY_AR.matcher(message)
        if (arMatcher.find()) {
            amount = ArabicNumberNormalizer.parseAmount(arMatcher.group(1).orEmpty())
            hasExplicitCurrency = true
        }

        // Try English amount with currency
        if (amount == null) {
            val enMatcher = AMOUNT_WITH_CURRENCY_EN.matcher(normalizedMsg)
            if (enMatcher.find()) {
                val groupVal = enMatcher.group(1) ?: enMatcher.group(2)
                amount = ArabicNumberNormalizer.parseAmount(groupVal.orEmpty())
                hasExplicitCurrency = true
            }
        }

        // Fallback: search for any standalone number > 0 if preceded by financial verb
        if (amount == null) {
            val genericNumberRegex = Pattern.compile("(?<!\\d)([\\d.,٠-٩]{2,})(?!\\d)")
            val genMatcher = genericNumberRegex.matcher(message)
            while (genMatcher.find()) {
                val candidateStr = genMatcher.group(1).orEmpty()
                val parsed = ArabicNumberNormalizer.parseAmount(candidateStr)
                if (parsed != null && parsed >= 5.0 && parsed != 1000.0.coerceAtLeast(parsed) || (parsed != null && parsed > 0.0 && parsed < 1_000_000.0)) {
                    amount = parsed
                    break
                }
            }
        }

        if (amount == null || amount <= 0.0) {
            return null
        }

        // 3. Extract card or account if present
        var accountOrCard: String? = null
        val cardMatcher = MASKED_CARD_REGEX.matcher(normalizedMsg)
        if (cardMatcher.find()) {
            val last4 = cardMatcher.group(1)
            accountOrCard = "*$last4"
        }

        val confidence = if (hasExplicitCurrency) {
            EventConfidence.HIGH
        } else {
            EventConfidence.MEDIUM
        }

        val sourceId = SourceIdentifierHelper.generate(sender, message, timestamp, originalSmsId)

        return PartialFinancialEvent(
            amount = amount,
            type = type,
            date = timestamp,
            source = FinancialEventSource.SMS,
            sourceIdentifier = sourceId,
            rawMessage = message,
            sender = sender.ifBlank { "Bank" },
            accountOrCard = accountOrCard,
            rawCounterparty = null,
            rawPhoneNumber = null,
            confidence = confidence
        )
    }
}

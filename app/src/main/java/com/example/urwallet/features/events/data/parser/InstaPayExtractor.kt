package com.example.urwallet.features.events.data.parser

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.EventConfidence
import com.example.urwallet.features.events.domain.model.FinancialEventSource
import com.example.urwallet.features.events.domain.model.PartialFinancialEvent
import java.util.Locale
import java.util.regex.Pattern

class InstaPayExtractor : FinancialEventExtractor {

    override val name: String = "InstaPay"

    private val EGYPTIAN_PHONE_REGEX = Pattern.compile("(01[0125][0-9]{8})")

    // Arabic patterns
    private val AR_TRANSFER_OUT = Pattern.compile("تم تحويل\\s*([\\d.,٠-٩]+)\\s*(?:ج\\.م|جنيه|EGP)?\\s*إلى\\s*(.+)", Pattern.CASE_INSENSITIVE)
    private val AR_TRANSFER_IN = Pattern.compile("تم استلام\\s*([\\d.,٠-٩]+)\\s*(?:ج\\.م|جنيه|EGP)?\\s*من\\s*(.+)", Pattern.CASE_INSENSITIVE)

    // English patterns
    private val EN_TRANSFER_OUT = Pattern.compile("(?:transfer of|transferred)\\s*(?:EGP|LE)?\\s*([\\d.,]+)\\s*(?:EGP|LE)?\\s*to\\s*(.+)", Pattern.CASE_INSENSITIVE)
    private val EN_TRANSFER_IN = Pattern.compile("(?:received|credited with)\\s*(?:EGP|LE)?\\s*([\\d.,]+)\\s*(?:EGP|LE)?\\s*from\\s*(.+)", Pattern.CASE_INSENSITIVE)

    override fun canHandle(sender: String, message: String): Boolean {
        val s = sender.lowercase(Locale.ROOT)
        val m = message.lowercase(Locale.ROOT)
        return s.contains("instapay") || m.contains("instapay") || m.contains("إنستاباي") || m.contains("انستاباي")
    }

    override fun extract(sender: String, message: String, timestamp: Long, originalSmsId: String?): PartialFinancialEvent? {
        val normalizedMsg = ArabicNumberNormalizer.normalizeDigits(message)

        var amount: Double? = null
        var type: TransactionType? = null
        var rawCounterparty: String? = null
        var rawPhone: String? = null

        // 1. Check Arabic Outbound (Expense)
        val arOutMatcher = AR_TRANSFER_OUT.matcher(message)
        if (arOutMatcher.find()) {
            amount = ArabicNumberNormalizer.parseAmount(arOutMatcher.group(1).orEmpty())
            type = TransactionType.EXPENSE
            rawCounterparty = cleanCounterparty(arOutMatcher.group(2))
        }

        // 2. Check Arabic Inbound (Income)
        if (amount == null) {
            val arInMatcher = AR_TRANSFER_IN.matcher(message)
            if (arInMatcher.find()) {
                amount = ArabicNumberNormalizer.parseAmount(arInMatcher.group(1).orEmpty())
                type = TransactionType.INCOME
                rawCounterparty = cleanCounterparty(arInMatcher.group(2))
            }
        }

        // 3. Check English Outbound
        if (amount == null) {
            val enOutMatcher = EN_TRANSFER_OUT.matcher(normalizedMsg)
            if (enOutMatcher.find()) {
                amount = ArabicNumberNormalizer.parseAmount(enOutMatcher.group(1).orEmpty())
                type = TransactionType.EXPENSE
                rawCounterparty = cleanCounterparty(enOutMatcher.group(2))
            }
        }

        // 4. Check English Inbound
        if (amount == null) {
            val enInMatcher = EN_TRANSFER_IN.matcher(normalizedMsg)
            if (enInMatcher.find()) {
                amount = ArabicNumberNormalizer.parseAmount(enInMatcher.group(1).orEmpty())
                type = TransactionType.INCOME
                rawCounterparty = cleanCounterparty(enInMatcher.group(2))
            }
        }

        if (amount == null || amount <= 0.0 || type == null) {
            return null
        }

        // Check if raw counterparty is or contains a phone number
        if (!rawCounterparty.isNullOrBlank()) {
            val phoneMatcher = EGYPTIAN_PHONE_REGEX.matcher(rawCounterparty)
            if (phoneMatcher.find()) {
                rawPhone = phoneMatcher.group(1)
            }
        }

        val sourceId = SourceIdentifierHelper.generate(sender, message, timestamp, originalSmsId)

        return PartialFinancialEvent(
            amount = amount,
            type = type,
            date = timestamp,
            source = FinancialEventSource.SMS,
            sourceIdentifier = sourceId,
            rawMessage = message,
            sender = if (sender.isBlank()) "InstaPay" else sender,
            accountOrCard = "InstaPay",
            rawCounterparty = rawCounterparty,
            rawPhoneNumber = rawPhone,
            confidence = EventConfidence.HIGH
        )
    }

    private fun cleanCounterparty(raw: String?): String? {
        if (raw == null) return null
        return raw.split("\n", ".", "بتاريخ", "on ").firstOrNull()?.trim()?.ifBlank { null }
    }
}

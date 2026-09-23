package com.example.urwallet.features.events.data.parser

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.EventConfidence
import com.example.urwallet.features.events.domain.model.FinancialEventSource
import com.example.urwallet.features.events.domain.model.PartialFinancialEvent
import java.util.Locale
import java.util.regex.Pattern

class CibExtractor : FinancialEventExtractor {

    override val name: String = "CIB"

    // Arabic patterns
    private val AR_PURCHASE = Pattern.compile("شراء بمبلغ\\s*([\\d.,٠-٩]+)\\s*(?:ج\\.م|جنيه|EGP)?.*?(?:بطاقة|بطاقتك|كارت)\\s*[*]?([\\d]{4})?.*?(?:لدى|من)\\s*([^.\\n]+)?", Pattern.CASE_INSENSITIVE)
    private val AR_WITHDRAWAL = Pattern.compile("سحب نقدي بمبلغ\\s*([\\d.,٠-٩]+)\\s*(?:ج\\.م|جنيه|EGP)?.*?(?:بطاقة|بطاقتك|كارت)?\\s*[*]?([\\d]{4})?", Pattern.CASE_INSENSITIVE)
    private val AR_DEPOSIT = Pattern.compile("(?:تم إيداع|ايداع نقدي بمبلغ|تم إضافة)\\s*([\\d.,٠-٩]+)\\s*(?:ج\\.م|جنيه|EGP)?.*?(?:حساب|حسابك)?\\s*[*]?([\\d]{4})?", Pattern.CASE_INSENSITIVE)

    // English patterns
    private val EN_PURCHASE = Pattern.compile("purchase of\\s*(?:EGP|LE)?\\s*([\\d.,]+).*?card\\s*[*]?([\\d]{4})?.*?(?:at|from)\\s*([^.\\n]+)?", Pattern.CASE_INSENSITIVE)
    private val EN_WITHDRAWAL = Pattern.compile("cash withdrawal of\\s*(?:EGP|LE)?\\s*([\\d.,]+).*?card\\s*[*]?([\\d]{4})?", Pattern.CASE_INSENSITIVE)
    private val EN_DEPOSIT = Pattern.compile("deposited with\\s*(?:EGP|LE)?\\s*([\\d.,]+).*?account\\s*[*]?([\\d]{4})?", Pattern.CASE_INSENSITIVE)

    override fun canHandle(sender: String, message: String): Boolean {
        val s = sender.lowercase(Locale.ROOT)
        val m = message.lowercase(Locale.ROOT)
        return s.contains("cib") || m.contains("cib") || m.contains("البنك التجاري الدولي")
    }

    override fun extract(sender: String, message: String, timestamp: Long, originalSmsId: String?): PartialFinancialEvent? {
        val normalizedMsg = ArabicNumberNormalizer.normalizeDigits(message)

        var amount: Double? = null
        var type: TransactionType? = null
        var cardOrAccount: String? = null
        var merchant: String? = null

        // 1. Arabic Purchase
        val arPurMatcher = AR_PURCHASE.matcher(message)
        if (arPurMatcher.find()) {
            amount = ArabicNumberNormalizer.parseAmount(arPurMatcher.group(1).orEmpty())
            type = TransactionType.EXPENSE
            val card = arPurMatcher.group(2)
            if (!card.isNullOrBlank()) cardOrAccount = "بطاقة *$card"
            merchant = arPurMatcher.group(3)?.trim()?.ifBlank { null }
        }

        // 2. Arabic Withdrawal
        if (amount == null) {
            val arWdMatcher = AR_WITHDRAWAL.matcher(message)
            if (arWdMatcher.find()) {
                amount = ArabicNumberNormalizer.parseAmount(arWdMatcher.group(1).orEmpty())
                type = TransactionType.EXPENSE
                val card = arWdMatcher.group(2)
                if (!card.isNullOrBlank()) cardOrAccount = "بطاقة *$card"
                merchant = "سحب ATM"
            }
        }

        // 3. Arabic Deposit
        if (amount == null) {
            val arDepMatcher = AR_DEPOSIT.matcher(message)
            if (arDepMatcher.find()) {
                amount = ArabicNumberNormalizer.parseAmount(arDepMatcher.group(1).orEmpty())
                type = TransactionType.INCOME
                val acc = arDepMatcher.group(2)
                if (!acc.isNullOrBlank()) cardOrAccount = "حساب *$acc"
            }
        }

        // 4. English Purchase
        if (amount == null) {
            val enPurMatcher = EN_PURCHASE.matcher(normalizedMsg)
            if (enPurMatcher.find()) {
                amount = ArabicNumberNormalizer.parseAmount(enPurMatcher.group(1).orEmpty())
                type = TransactionType.EXPENSE
                val card = enPurMatcher.group(2)
                if (!card.isNullOrBlank()) cardOrAccount = "Card *$card"
                merchant = enPurMatcher.group(3)?.trim()?.ifBlank { null }
            }
        }

        // 5. English Withdrawal
        if (amount == null) {
            val enWdMatcher = EN_WITHDRAWAL.matcher(normalizedMsg)
            if (enWdMatcher.find()) {
                amount = ArabicNumberNormalizer.parseAmount(enWdMatcher.group(1).orEmpty())
                type = TransactionType.EXPENSE
                val card = enWdMatcher.group(2)
                if (!card.isNullOrBlank()) cardOrAccount = "Card *$card"
                merchant = "ATM Cash"
            }
        }

        // 6. English Deposit
        if (amount == null) {
            val enDepMatcher = EN_DEPOSIT.matcher(normalizedMsg)
            if (enDepMatcher.find()) {
                amount = ArabicNumberNormalizer.parseAmount(enDepMatcher.group(1).orEmpty())
                type = TransactionType.INCOME
                val acc = enDepMatcher.group(2)
                if (!acc.isNullOrBlank()) cardOrAccount = "Account *$acc"
            }
        }

        if (amount == null || amount <= 0.0 || type == null) {
            return null
        }

        val sourceId = SourceIdentifierHelper.generate(sender, message, timestamp, originalSmsId)

        return PartialFinancialEvent(
            amount = amount,
            type = type,
            date = timestamp,
            source = FinancialEventSource.SMS,
            sourceIdentifier = sourceId,
            rawMessage = message,
            sender = if (sender.isBlank()) "CIB" else sender,
            accountOrCard = cardOrAccount,
            rawCounterparty = merchant,
            rawPhoneNumber = null,
            confidence = EventConfidence.HIGH
        )
    }
}

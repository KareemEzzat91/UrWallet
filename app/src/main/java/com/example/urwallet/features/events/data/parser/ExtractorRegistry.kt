package com.example.urwallet.features.events.data.parser

import com.example.urwallet.features.events.domain.model.EventConfidence
import com.example.urwallet.features.events.domain.model.PartialFinancialEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractorRegistry @Inject constructor() {

    private val extractors: List<FinancialEventExtractor> = listOf(
        InstaPayExtractor(),
        CibExtractor(),
        GenericFinancialExtractor()
    )

    /**
     * Parses an SMS message.
     * 1. Runs [SmsDetectionFilter] to weed out OTPs, balance queries, and marketing.
     * 2. Tries bank-specific extractors first, then falls back to generic.
     * 3. Drops any result with [EventConfidence.LOW] (Refinement 2: LOW confidence never enters Inbox).
     */
    fun extract(
        sender: String,
        message: String,
        timestamp: Long,
        originalSmsId: String? = null
    ): PartialFinancialEvent? {
        if (!SmsDetectionFilter.isPotentialFinancialSms(message)) {
            return null
        }

        for (extractor in extractors) {
            if (extractor.canHandle(sender, message)) {
                val candidate = extractor.extract(sender, message, timestamp, originalSmsId)
                if (candidate != null) {
                    // Refinement 2: LOW confidence events do not enter the Inbox
                    if (candidate.confidence == EventConfidence.LOW) {
                        return null
                    }
                    return candidate
                }
            }
        }

        return null
    }
}

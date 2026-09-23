package com.example.urwallet.features.events.data.parser

import com.example.urwallet.features.events.domain.model.PartialFinancialEvent

interface FinancialEventExtractor {
    val name: String
    fun canHandle(sender: String, message: String): Boolean
    fun extract(sender: String, message: String, timestamp: Long, originalSmsId: String? = null): PartialFinancialEvent?
}

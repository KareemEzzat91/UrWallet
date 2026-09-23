package com.example.urwallet.features.events

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.data.parser.CibExtractor
import com.example.urwallet.features.events.data.parser.ExtractorRegistry
import com.example.urwallet.features.events.data.parser.GenericFinancialExtractor
import com.example.urwallet.features.events.data.parser.InstaPayExtractor
import com.example.urwallet.features.events.domain.model.EventConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialEventExtractorTest {

    private val instaPayExtractor = InstaPayExtractor()
    private val cibExtractor = CibExtractor()
    private val genericExtractor = GenericFinancialExtractor()
    private val registry = ExtractorRegistry()

    @Test
    fun `InstaPayExtractor extracts outbound transfer with phone number`() {
        val sender = "InstaPay"
        val message = "تم تحويل 500.00 ج.م إلى 01012345678 بنجاح"
        val timestamp = 1727000000000L

        assertTrue(instaPayExtractor.canHandle(sender, message))
        val event = instaPayExtractor.extract(sender, message, timestamp)

        assertNotNull(event)
        assertEquals(500.00, event!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, event.type)
        assertEquals("01012345678", event.rawPhoneNumber)
        assertEquals(EventConfidence.HIGH, event.confidence)
    }

    @Test
    fun `InstaPayExtractor extracts inbound transfer with person name`() {
        val sender = "InstaPay"
        val message = "تم استلام 1,250.00 ج.م من سارة علي عبر انستاباي"
        val timestamp = 1727000000000L

        val event = instaPayExtractor.extract(sender, message, timestamp)

        assertNotNull(event)
        assertEquals(1250.00, event!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, event.type)
        assertTrue(event.rawCounterparty?.contains("سارة علي") == true)
        assertEquals(EventConfidence.HIGH, event.confidence)
    }

    @Test
    fun `CibExtractor extracts Arabic card purchase and merchant`() {
        val sender = "CIB"
        val message = "شراء بمبلغ 350.00 ج.م باستخدام بطاقتك *1234 لدى كارفور المعادي"
        val timestamp = 1727000000000L

        assertTrue(cibExtractor.canHandle(sender, message))
        val event = cibExtractor.extract(sender, message, timestamp)

        assertNotNull(event)
        assertEquals(350.00, event!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, event.type)
        assertTrue(event.accountOrCard?.contains("1234") == true)
        assertTrue(event.rawCounterparty?.contains("كارفور") == true)
        assertEquals(EventConfidence.HIGH, event.confidence)
    }

    @Test
    fun `CibExtractor extracts English card purchase`() {
        val sender = "CIB"
        val message = "CIB: Purchase of EGP 150.75 with card *9876 at STARBUCKS"
        val timestamp = 1727000000000L

        val event = cibExtractor.extract(sender, message, timestamp)

        assertNotNull(event)
        assertEquals(150.75, event!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, event.type)
        assertTrue(event.accountOrCard?.contains("9876") == true)
        assertTrue(event.rawCounterparty?.contains("STARBUCKS") == true)
    }

    @Test
    fun `CibExtractor extracts cash withdrawal`() {
        val sender = "CIB"
        val message = "سحب نقدي بمبلغ 2,000 ج.م من بطاقة *4321"
        val timestamp = 1727000000000L

        val event = cibExtractor.extract(sender, message, timestamp)

        assertNotNull(event)
        assertEquals(2000.00, event!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, event.type)
    }

    @Test
    fun `GenericFinancialExtractor extracts fallback bank debit`() {
        val sender = "BanqueMisr"
        val message = "تم خصم مبلغ 420.50 ج.م من حسابك *5544"
        val timestamp = 1727000000000L

        val event = genericExtractor.extract(sender, message, timestamp)

        assertNotNull(event)
        assertEquals(420.50, event!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, event.type)
        assertTrue(event.accountOrCard?.contains("5544") == true)
    }

    @Test
    fun `GenericFinancialExtractor extracts fallback bank deposit`() {
        val sender = "NBE"
        val message = "تم إضافة مبلغ 8,000 ج.م إلى حسابك"
        val timestamp = 1727000000000L

        val event = genericExtractor.extract(sender, message, timestamp)

        assertNotNull(event)
        assertEquals(8000.00, event!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, event.type)
    }

    @Test
    fun `ExtractorRegistry returns null for non-financial or OTP SMS`() {
        val sender = "Bank"
        val otpMessage = "رمز التأكيد الخاص بك هو 993182"
        val event = registry.extract(sender, otpMessage, System.currentTimeMillis())
        assertNull(event)
    }

    @Test
    fun `ExtractorRegistry successfully orchestrates bank extraction`() {
        val sender = "CIB"
        val message = "شراء بمبلغ 99.00 ج.م باستخدام بطاقتك *1234 لدى ماكدونالدز"
        val event = registry.extract(sender, message, 1727000000000L)

        assertNotNull(event)
        assertEquals(99.00, event!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, event.type)
    }
}

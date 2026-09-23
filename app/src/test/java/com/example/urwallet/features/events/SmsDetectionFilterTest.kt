package com.example.urwallet.features.events

import com.example.urwallet.features.events.data.parser.SmsDetectionFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsDetectionFilterTest {

    @Test
    fun `isPotentialFinancialSms rejects OTP messages`() {
        val otp1 = "رمز التحقق الخاص بك هو: 849201. لا تشارك هذا الرمز مع أي شخص."
        val otp2 = "Your OTP is 123456. CIB security code."
        val otp3 = "كود التأكيد لعملية الشراء هو 7741. صالح لمدة 5 دقائق."

        assertFalse(SmsDetectionFilter.isPotentialFinancialSms(otp1))
        assertFalse(SmsDetectionFilter.isPotentialFinancialSms(otp2))
        assertFalse(SmsDetectionFilter.isPotentialFinancialSms(otp3))
    }

    @Test
    fun `isPotentialFinancialSms rejects promotional marketing messages`() {
        val promo1 = "عرض خاص لعملاء البنك! قسط مشترياتك حتى 12 شهر بدون فوائد. اشترك الآن."
        val promo2 = "Special offer! Get discount up to 20% when you use your card."

        assertFalse(SmsDetectionFilter.isPotentialFinancialSms(promo1))
        assertFalse(SmsDetectionFilter.isPotentialFinancialSms(promo2))
    }

    @Test
    fun `isPotentialFinancialSms rejects pure balance inquiries without transaction`() {
        val balanceMsg = "رصيدك الحالي هو 15,200.00 ج.م"
        assertFalse(SmsDetectionFilter.isPotentialFinancialSms(balanceMsg))

        val balanceMsgEn = "Your balance is 5,000 EGP"
        assertFalse(SmsDetectionFilter.isPotentialFinancialSms(balanceMsgEn))
    }

    @Test
    fun `isPotentialFinancialSms accepts valid financial transactions`() {
        val debitSms = "تم خصم مبلغ 450.00 ج.م من بطاقتك المنتهية برقم 1234 في كارفور"
        val transferSms = "تم تحويل مبلغ 1,000 جم بنجاح إلى 01099887766"
        val creditSms = "تم استلام مبلغ 5,500.00 جم في حسابك"
        val purchaseEn = "Purchase of EGP 200.00 with card ending 5678 at STARBUCKS"

        assertTrue(SmsDetectionFilter.isPotentialFinancialSms(debitSms))
        assertTrue(SmsDetectionFilter.isPotentialFinancialSms(transferSms))
        assertTrue(SmsDetectionFilter.isPotentialFinancialSms(creditSms))
        assertTrue(SmsDetectionFilter.isPotentialFinancialSms(purchaseEn))
    }

    @Test
    fun `isPotentialFinancialSms rejects message with financial keywords but without numbers`() {
        val noDigits = "تم خصم الرسوم الإدارية الشهرية"
        assertFalse(SmsDetectionFilter.isPotentialFinancialSms(noDigits))
    }
}

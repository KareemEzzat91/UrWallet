package com.example.urwallet.features.events

import com.example.urwallet.features.events.data.parser.ArabicNumberNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArabicNumberNormalizerTest {

    @Test
    fun `normalizeDigits converts Eastern Arabic numerals to Western Arabic`() {
        val input = "١٢٣٤٥٦٧٨٩٠"
        val expected = "1234567890"
        assertEquals(expected, ArabicNumberNormalizer.normalizeDigits(input))
    }

    @Test
    fun `normalizeDigits preserves existing Western Arabic digits and text`() {
        val input = "Order 1234: المبلغ ٥٠٠ جنيه"
        val expected = "Order 1234: المبلغ 500 جنيه"
        assertEquals(expected, ArabicNumberNormalizer.normalizeDigits(input))
    }

    @Test
    fun `parseAmount handles standard comma thousands separator`() {
        val input = "1,500.50"
        val amount = ArabicNumberNormalizer.parseAmount(input)
        assertNotNull(amount)
        assertEquals(1500.50, amount!!, 0.001)
    }

    @Test
    fun `parseAmount handles Eastern Arabic digits with commas`() {
        val input = "١,٢٥٠٫٧٥"
        val amount = ArabicNumberNormalizer.parseAmount(input)
        assertNotNull(amount)
        assertEquals(1250.75, amount!!, 0.001)
    }

    @Test
    fun `parseAmount returns null when invalid numeric string`() {
        val input = "غير صالح"
        val amount = ArabicNumberNormalizer.parseAmount(input)
        assertNull(amount)
    }

    @Test
    fun `parseAmount extracts standard decimal numbers correctly`() {
        val input = "349.99"
        val amount = ArabicNumberNormalizer.parseAmount(input)
        assertNotNull(amount)
        assertEquals(349.99, amount!!, 0.001)
    }
}

package com.example.urwallet.features.events

import com.example.urwallet.features.events.data.parser.SourceIdentifierHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SourceIdentifierHelperTest {

    @Test
    fun `generate produces identical hash regardless of when execution occurs (Idempotency)`() {
        val sender = "CIB"
        val message = "Purchase of EGP 100 with card *1234"
        val smsTimestamp = 1727000000000L

        val hash1 = SourceIdentifierHelper.generate(sender, message, smsTimestamp)
        Thread.sleep(15) // Simulate elapsed time during scanning
        val hash2 = SourceIdentifierHelper.generate(sender, message, smsTimestamp)

        assertEquals("Same SMS must produce identical sourceIdentifier across scans", hash1, hash2)
    }

    @Test
    fun `generate prefers originalSmsId when provided`() {
        val sender = "InstaPay"
        val message = "تم تحويل 200 ج.م"
        val smsTimestamp = 1727000000000L
        val originalSmsId = "987654"

        val id = SourceIdentifierHelper.generate(sender, message, smsTimestamp, originalSmsId)
        assertEquals("sms_id_987654", id)
    }

    @Test
    fun `generate produces different hashes for different messages or timestamps`() {
        val sender = "CIB"
        val message1 = "Purchase of EGP 100 with card *1234"
        val message2 = "Purchase of EGP 200 with card *1234"
        val timestamp1 = 1727000000000L
        val timestamp2 = 1727000050000L

        val id1 = SourceIdentifierHelper.generate(sender, message1, timestamp1)
        val id2 = SourceIdentifierHelper.generate(sender, message2, timestamp1)
        val id3 = SourceIdentifierHelper.generate(sender, message1, timestamp2)

        assertNotEquals(id1, id2)
        assertNotEquals(id1, id3)
    }
}

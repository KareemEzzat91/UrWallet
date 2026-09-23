package com.example.urwallet.features.events.data.parser

import java.security.MessageDigest
import java.util.Locale

object SourceIdentifierHelper {

    /**
     * Generates a stable, idempotent identifier for an incoming or scanned SMS.
     *
     * 1. If [originalSmsId] is available (from Android Telephony provider), uses "sms_id_${originalSmsId}".
     * 2. Otherwise generates a deterministic SHA-256 fingerprint based EXCLUSIVELY on immutable source fields:
     *    normalized sender + network SMS timestamp + normalized message body.
     *
     * STRICT INVARIANT: Never incorporates the current/processing system timestamp.
     */
    fun generate(
        sender: String,
        message: String,
        smsTimestamp: Long,
        originalSmsId: String? = null
    ): String {
        if (!originalSmsId.isNullOrBlank()) {
            return "sms_id_${originalSmsId.trim()}"
        }

        val normalizedSender = sender.trim().lowercase(Locale.ROOT)
        val normalizedBody = message.trim().replace("\r\n", "\n")
        val rawSeed = "$normalizedSender:$smsTimestamp:$normalizedBody"

        val bytes = MessageDigest.getInstance("SHA-256").digest(rawSeed.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

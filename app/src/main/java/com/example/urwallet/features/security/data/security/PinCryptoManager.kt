package com.example.urwallet.features.security.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles cryptographic operations for the App Lock PIN:
 * - Generates 16-byte cryptographically secure random salts.
 * - Derives 256-bit hashes using PBKDF2WithHmacSHA256 (10,000 iterations).
 * - Performs constant-time equality comparisons via [MessageDigest.isEqual] to resist timing attacks.
 *
 * Note: PBKDF2 is a password-based key derivation function (one-way hash), NOT encryption.
 * The raw PIN is never stored or logged.
 */
@Singleton
class PinCryptoManager @Inject constructor() {

    private val secureRandom = SecureRandom()

    /**
     * Generates a new 16-byte cryptographically random salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit hash from the given [pin] and [salt] using PBKDF2WithHmacSHA256.
     */
    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    /**
     * Verifies whether the [candidatePin] matches the stored [storedSaltBase64] and [storedHashBase64].
     * Uses [MessageDigest.isEqual] for constant-time evaluation to prevent side-channel timing attacks.
     */
    fun verifyPin(candidatePin: String, storedSaltBase64: String, storedHashBase64: String): Boolean {
        return try {
            val salt = decodeBase64(storedSaltBase64)
            val expectedHash = decodeBase64(storedHashBase64)
            val candidateHash = hashPin(candidatePin, salt)
            MessageDigest.isEqual(candidateHash, expectedHash)
        } catch (_: Exception) {
            false
        }
    }

    fun encodeBase64(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun decodeBase64(base64Str: String): ByteArray {
        return Base64.getDecoder().decode(base64Str)
    }

    companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ITERATIONS = 10_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_LENGTH_BYTES = 16
    }
}

package com.example.urwallet.features.security

import com.example.urwallet.features.security.data.security.PinCryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinCryptoManagerTest {

    private lateinit var cryptoManager: PinCryptoManager

    @Before
    fun setUp() {
        cryptoManager = PinCryptoManager()
    }

    @Test
    fun generateSalt_producesUnique16ByteSalts() {
        val salt1 = cryptoManager.generateSalt()
        val salt2 = cryptoManager.generateSalt()

        assertEquals(16, salt1.size)
        assertEquals(16, salt2.size)
        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun hashPin_samePinAndSalt_producesIdenticalHash() {
        val salt = cryptoManager.generateSalt()
        val pin = "1234"

        val hash1 = cryptoManager.hashPin(pin, salt)
        val hash2 = cryptoManager.hashPin(pin, salt)

        assertEquals(32, hash1.size) // 256 bits = 32 bytes
        assertTrue(hash1.contentEquals(hash2))
    }

    @Test
    fun hashPin_differentPinSameSalt_producesDifferentHash() {
        val salt = cryptoManager.generateSalt()
        val hash1 = cryptoManager.hashPin("1234", salt)
        val hash2 = cryptoManager.hashPin("5678", salt)

        assertFalse(hash1.contentEquals(hash2))
    }

    @Test
    fun verifyPin_correctPin_returnsTrue() {
        val salt = cryptoManager.generateSalt()
        val hash = cryptoManager.hashPin("4321", salt)

        val saltBase64 = cryptoManager.encodeBase64(salt)
        val hashBase64 = cryptoManager.encodeBase64(hash)

        val result = cryptoManager.verifyPin("4321", saltBase64, hashBase64)
        assertTrue(result)
    }

    @Test
    fun verifyPin_incorrectPin_returnsFalse() {
        val salt = cryptoManager.generateSalt()
        val hash = cryptoManager.hashPin("4321", salt)

        val saltBase64 = cryptoManager.encodeBase64(salt)
        val hashBase64 = cryptoManager.encodeBase64(hash)

        val result = cryptoManager.verifyPin("0000", saltBase64, hashBase64)
        assertFalse(result)
    }

    @Test
    fun verifyPin_invalidBase64_returnsFalseSafelyWithoutCrashing() {
        val result = cryptoManager.verifyPin("1234", "invalid-salt", "invalid-hash")
        assertFalse(result)
    }
}

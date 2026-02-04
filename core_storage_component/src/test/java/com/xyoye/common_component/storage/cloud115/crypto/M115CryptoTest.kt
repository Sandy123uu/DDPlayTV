package com.xyoye.common_component.storage.cloud115.crypto

import java.security.SecureRandom
import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M115CryptoTest {
    private val random = SecureRandom()

    @Test
    fun encode_payload_isReversible_byServerDecodeTransform() {
        repeat(16) {
            val key = M115Crypto.generateKey()
            val plain = randomBytes(random.nextInt(1024))

            val payload = M115Crypto.encodePayload(plain, key)
            val decodedKey = payload.copyOfRange(0, 16)
            val decodedPlain = decodeRequestPayload(payload, key)

            assertArrayEquals(key, decodedKey)
            assertArrayEquals(plain, decodedPlain)
        }
    }

    @Test
    fun encode_usesRandomRsaPadding() {
        val key = M115Crypto.generateKey()
        val plain = randomBytes(256)

        val encoded1 = M115Crypto.encode(plain, key)
        val encoded2 = M115Crypto.encode(plain, key)
        assertNotEquals(encoded1, encoded2)

        val encrypted = Base64.getDecoder().decode(encoded1)
        assertTrue(encrypted.isNotEmpty())
        assertEquals(0, encrypted.size % 128)
    }

    @Test
    fun decode_payload_roundTrips_withSyntheticDecryptedBlob() {
        repeat(16) {
            val requestKey = M115Crypto.generateKey()
            val serverKey = M115Crypto.generateKey()
            val plain = randomBytes(random.nextInt(1024))

            val decrypted = buildSyntheticDecryptedBlob(plain, requestKey, serverKey)
            val decoded = M115Crypto.decodePayload(decrypted, requestKey)
            assertArrayEquals(plain, decoded)
        }
    }

    @Test
    fun decode_throws_onInvalidInput() {
        val key = M115Crypto.generateKey()

        assertTrue(runCatching { M115Crypto.decode("not_base64", key) }.isFailure)
        assertTrue(runCatching { M115Crypto.decode("", key) }.isFailure)
        assertTrue(runCatching { M115Crypto.decode("AA==", key) }.isFailure)
        assertTrue(runCatching { M115Crypto.decode("AA==", ByteArray(15)) }.isFailure)
        assertTrue(runCatching { M115Crypto.encode(byteArrayOf(1, 2, 3), ByteArray(15)) }.isFailure)
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

    private fun decodeRequestPayload(
        payloadWithKey: ByteArray,
        requestKey: ByteArray
    ): ByteArray {
        require(payloadWithKey.size >= 16) { "invalid payload" }
        val payload = payloadWithKey.copyOfRange(16, payloadWithKey.size)
        M115Crypto.xorTransform(payload, 0, payload.size, M115Crypto.xorClientKey)
        M115Crypto.reverseBytes(payload, 0, payload.size)
        M115Crypto.xorTransform(payload, 0, payload.size, M115Crypto.xorDeriveKey(requestKey, 4))
        return payload
    }

    private fun buildSyntheticDecryptedBlob(
        plain: ByteArray,
        requestKey: ByteArray,
        serverKey: ByteArray
    ): ByteArray {
        val payload = plain.copyOf()
        M115Crypto.xorTransform(payload, 0, payload.size, M115Crypto.xorDeriveKey(requestKey, 4))
        M115Crypto.reverseBytes(payload, 0, payload.size)
        M115Crypto.xorTransform(payload, 0, payload.size, M115Crypto.xorDeriveKey(serverKey, 12))

        val buf = ByteArray(16 + payload.size)
        System.arraycopy(serverKey, 0, buf, 0, 16)
        System.arraycopy(payload, 0, buf, 16, payload.size)
        return buf
    }
}

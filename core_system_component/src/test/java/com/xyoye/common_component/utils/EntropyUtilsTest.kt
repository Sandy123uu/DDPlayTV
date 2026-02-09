package com.xyoye.common_component.utils

import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
class EntropyUtilsTest {
    @Test
    fun string2Md5ReturnsKnownDigest() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", EntropyUtils.string2Md5("abc"))
    }

    @Test
    fun string2Md5ReturnsEmptyForNullAndBlank() {
        assertEquals("", EntropyUtils.string2Md5(null))
        assertEquals("", EntropyUtils.string2Md5(""))
    }

    @Test
    fun aesEncodeAndDecodeV2RoundTrip() {
        val key = "1234567890abcdef"
        val plainText = "player-config-encrypt"

        val encrypted = EntropyUtils.aesEncode(key, plainText, Base64.NO_WRAP)

        assertNotNull(encrypted)
        assertEquals(plainText, EntropyUtils.aesDecode(key, encrypted!!, Base64.NO_WRAP))
    }

    @Test
    fun aesDecodeReturnsNullWhenBase64IsInvalid() {
        val decrypted = EntropyUtils.aesDecode("1234567890abcdef", "@@not-base64@@", Base64.NO_WRAP)

        assertNull(decrypted)
    }

    @Test
    fun aesDecodeSupportsLegacyCbcPayload() {
        val key = "1234567890abcdef"
        val plainText = "legacy-cbc-compatible"
        val legacyCipher = encryptLegacyCbc(key, plainText)
        val encoded = Base64.encodeToString(legacyCipher, Base64.NO_WRAP)

        val decrypted = EntropyUtils.aesDecode(key, encoded, Base64.NO_WRAP)

        assertEquals(plainText, decrypted)
    }

    @Test
    fun aesDecodeSupportsLegacyFallbackDefaultKey() {
        val legacyDefaultKey = "IiHcoJPwt5TCrR2r"
        val plainText = "legacy-default-key"
        val legacyCipher = encryptLegacyCbc(legacyDefaultKey, plainText)
        val encoded = Base64.encodeToString(legacyCipher, Base64.NO_WRAP)

        val decrypted =
            EntropyUtils.aesDecode(
                key = null,
                content = encoded,
                base64Flag = Base64.NO_WRAP,
                allowLegacyDefaultKeyFallback = true,
            )

        assertEquals(plainText, decrypted)
    }

    @Test
    fun aesDecodeReturnsNullForInvalidLegacyPadding() {
        val invalidLegacyCipher = ByteArray(16) { 0 }
        val encoded = Base64.encodeToString(invalidLegacyCipher, Base64.NO_WRAP)

        val decrypted =
            EntropyUtils.aesDecode(
                key = "1234567890abcdef",
                content = encoded,
                base64Flag = Base64.NO_WRAP,
            )

        assertNull(decrypted)
    }

    private fun encryptLegacyCbc(
        key: String,
        plainText: String
    ): ByteArray {
        val blockSize = 16
        val source = plainText.toByteArray(Charsets.UTF_8)
        val padding = blockSize - (source.size % blockSize)
        val padded = source + ByteArray(padding) { padding.toByte() }

        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(ByteArray(blockSize)),
        )
        return cipher.doFinal(padded)
    }
}

package com.xyoye.common_component.utils

import android.text.TextUtils
import android.util.Base64
import com.xyoye.common_component.extension.toHexString
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Created by xyoye on 2022/1/14
 */
object EntropyUtils {
    const val AES_VERSION_LEGACY_CBC_V1 = 1
    const val AES_VERSION_GCM_V2 = 2

    private const val LEGACY_DEFAULT_AES_KEY = "IiHcoJPwt5TCrR2r"

    private val secureRandom = SecureRandom()

    private val v2Magic =
        byteArrayOf(
            'D'.code.toByte(),
            'D'.code.toByte(),
            'P'.code.toByte(),
            'T'.code.toByte(),
        )
    private const val v2HeaderSizeBytes = 6
    private const val v2IvSizeBytes = 12
    private const val v2TagSizeBits = 128
    private const val LEGACY_CBC_BLOCK_SIZE_BYTES = 16

    /**
     * md5加密字符串
     */
    fun string2Md5(string: String?): String {
        if (TextUtils.isEmpty(string)) {
            return ""
        }

        val messageDigest = MessageDigest.getInstance("MD5")
        messageDigest.update(string!!.toByteArray())
        return messageDigest.digest().toHexString()
    }

    /**
     * 获取文件MD5值
     */
    fun file2Md5(file: File): String? {
        if (!file.exists() || !file.isFile || file.length() == 0L) {
            return null
        }

        val messageDigest = MessageDigest.getInstance("MD5")
        var fileInputStream: FileInputStream? = null
        val buffer = ByteArray(1024)
        var length: Int
        try {
            fileInputStream = FileInputStream(file)
            while (fileInputStream.read(buffer).also { length = it } != -1) {
                messageDigest.update(buffer, 0, length)
            }
            return messageDigest.digest().toHexString()
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedException(
                e,
                "EntropyUtils.file2Md5",
                "计算文件MD5失败: ${file.absolutePath}",
            )
        } finally {
            IOUtils.closeIO(fileInputStream)
        }

        return null
    }

    /**
     * AES加密字符串
     */
    fun aesEncode(
        key: String?,
        content: String,
        base64Flag: Int = Base64.DEFAULT,
        version: Int = AES_VERSION_GCM_V2,
        allowLegacyDefaultKeyFallback: Boolean = false
    ): String? {
        val normalizedKey =
            when {
                key.isNullOrBlank().not() -> key!!
                allowLegacyDefaultKeyFallback && version == AES_VERSION_LEGACY_CBC_V1 -> LEGACY_DEFAULT_AES_KEY
                else -> null
            } ?: run {
                ErrorReportHelper.postCatchedException(
                    IllegalArgumentException("AES key is empty"),
                    "EntropyUtils.aesEncode",
                    "AES加密失败：key为空",
                )
                return null
            }

        return runCatching {
            when (version) {
                AES_VERSION_GCM_V2 -> encodeV2(normalizedKey, content)
                // Legacy CBC is kept only for read compatibility. New payloads always use v2.
                AES_VERSION_LEGACY_CBC_V1 -> encodeV2(normalizedKey, content)
                else -> throw IllegalArgumentException("Unknown aes version: $version")
            }
        }.mapCatching { Base64.encodeToString(it, base64Flag) }
            .onFailure {
                ErrorReportHelper.postCatchedException(
                    it,
                    "EntropyUtils.aesEncode",
                    "AES加密失败：version=$version",
                )
            }.getOrNull()
    }

    /**
     * AES解密字符串
     */
    fun aesDecode(
        key: String?,
        content: String,
        base64Flag: Int = Base64.DEFAULT,
        allowLegacyDefaultKeyFallback: Boolean = false
    ): String? {
        val decoded =
            runCatching { Base64.decode(content, base64Flag) }
                .onFailure {
                    ErrorReportHelper.postCatchedException(
                        it,
                        "EntropyUtils.aesDecode",
                        "AES解密失败：Base64解码失败",
                    )
                }.getOrNull() ?: return null

        val normalizedKey = key?.takeIf { it.isNotBlank() }
        if (isV2Payload(decoded)) {
            if (normalizedKey == null) {
                ErrorReportHelper.postCatchedException(
                    IllegalArgumentException("AES key is empty"),
                    "EntropyUtils.aesDecode",
                    "AES解密失败：key为空",
                )
                return null
            }
            return runCatching { decodeV2(normalizedKey, decoded) }
                .onFailure {
                    ErrorReportHelper.postCatchedException(
                        it,
                        "EntropyUtils.aesDecode",
                        "AES解密失败：version=${AES_VERSION_GCM_V2}",
                    )
                }.getOrNull()
        }

        val legacyResult =
            runCatching {
                if (normalizedKey != null) {
                    decodeLegacyCbc(normalizedKey, decoded)
                } else {
                    throw IllegalArgumentException("Legacy key is empty")
                }
            }

        if (legacyResult.isSuccess) {
            return legacyResult.getOrNull()
        }

        if (allowLegacyDefaultKeyFallback) {
            return runCatching { decodeLegacyCbc(LEGACY_DEFAULT_AES_KEY, decoded) }
                .onFailure {
                    ErrorReportHelper.postCatchedException(
                        it,
                        "EntropyUtils.aesDecode",
                        "AES解密失败：legacy fallback",
                    )
                }.getOrNull()
        }

        legacyResult.exceptionOrNull()?.let {
            ErrorReportHelper.postCatchedException(
                it,
                "EntropyUtils.aesDecode",
                "AES解密失败：version=${AES_VERSION_LEGACY_CBC_V1}",
            )
        }
        return null
    }

    private fun isV2Payload(payload: ByteArray): Boolean {
        if (payload.size < v2HeaderSizeBytes) return false
        if (payload[0] != v2Magic[0]) return false
        if (payload[1] != v2Magic[1]) return false
        if (payload[2] != v2Magic[2]) return false
        if (payload[3] != v2Magic[3]) return false
        val version = payload[4].toInt() and 0xFF
        return version == AES_VERSION_GCM_V2
    }

    private fun encodeV2(
        key: String,
        content: String
    ): ByteArray {
        val secretKey = createV2AesKey(key)
        val iv =
            ByteArray(v2IvSizeBytes).apply {
                secureRandom.nextBytes(this)
            }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(v2TagSizeBits, iv))
        val cipherText = cipher.doFinal(content.toByteArray(Charsets.UTF_8))

        val payload =
            ByteArray(v2HeaderSizeBytes + iv.size + cipherText.size).apply {
                System.arraycopy(v2Magic, 0, this, 0, v2Magic.size)
                this[4] = AES_VERSION_GCM_V2.toByte()
                this[5] = iv.size.toByte()
                System.arraycopy(iv, 0, this, v2HeaderSizeBytes, iv.size)
                System.arraycopy(cipherText, 0, this, v2HeaderSizeBytes + iv.size, cipherText.size)
            }
        return payload
    }

    private fun decodeV2(
        key: String,
        payload: ByteArray
    ): String {
        if (payload.size < v2HeaderSizeBytes) {
            throw IllegalArgumentException("Invalid v2 payload length: ${payload.size}")
        }
        val ivLength = payload[5].toInt() and 0xFF
        if (ivLength <= 0) {
            throw IllegalArgumentException("Invalid v2 iv length: $ivLength")
        }
        val ivStart = v2HeaderSizeBytes
        val cipherStart = ivStart + ivLength
        if (cipherStart >= payload.size) {
            throw IllegalArgumentException("Invalid v2 payload length: ${payload.size}")
        }
        val iv = payload.copyOfRange(ivStart, cipherStart)
        val cipherText = payload.copyOfRange(cipherStart, payload.size)

        val secretKey = createV2AesKey(key)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(v2TagSizeBits, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun decodeLegacyCbc(
        key: String,
        cipherText: ByteArray
    ): String {
        if (cipherText.isEmpty() || cipherText.size % LEGACY_CBC_BLOCK_SIZE_BYTES != 0) {
            throw IllegalArgumentException("Invalid legacy cipher length: ${cipherText.size}")
        }
        val secretKey = createLegacyAesKey(key)
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ByteArray(LEGACY_CBC_BLOCK_SIZE_BYTES)))
        val rawPlainText = cipher.doFinal(cipherText)
        val plainText = removePkcs5Padding(rawPlainText)
        return String(plainText, Charsets.UTF_8)
    }

    private fun removePkcs5Padding(plainText: ByteArray): ByteArray {
        if (plainText.isEmpty() || plainText.size % LEGACY_CBC_BLOCK_SIZE_BYTES != 0) {
            throw IllegalArgumentException("Invalid legacy plain text length: ${plainText.size}")
        }
        val paddingLength = plainText.last().toInt() and 0xFF
        if (
            paddingLength <= 0 ||
            paddingLength > LEGACY_CBC_BLOCK_SIZE_BYTES ||
            paddingLength > plainText.size
        ) {
            throw IllegalArgumentException("Invalid legacy padding length: $paddingLength")
        }
        val paddingStart = plainText.size - paddingLength
        for (index in paddingStart until plainText.size) {
            if ((plainText[index].toInt() and 0xFF) != paddingLength) {
                throw IllegalArgumentException("Invalid legacy padding content")
            }
        }
        return plainText.copyOfRange(0, paddingStart)
    }

    private fun createV2AesKey(key: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        val aesKey = digest.copyOf(16)
        return SecretKeySpec(aesKey, "AES")
    }

    private fun createLegacyAesKey(key: String): SecretKeySpec {
        val rawKey = key.toByteArray(Charsets.UTF_8)
        if (rawKey.size != 16 && rawKey.size != 24 && rawKey.size != 32) {
            throw IllegalArgumentException("Invalid legacy AES key length: ${rawKey.size}")
        }
        return SecretKeySpec(rawKey, "AES")
    }
}

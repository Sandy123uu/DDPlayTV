package com.xyoye.common_component.storage.cloud115.crypto

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.SecureRandom

/**
 * m115 加解密（用于 115 Cloud 播放直链接口）。
 *
 * 说明：
 * - 算法参考社区 MIT 实现（SheltonZhu/115driver）。
 * - 本实现仅用于“请求 data 加密 + 响应 data 解密”，不承诺与服务器侧完全对称。
 */
object M115Crypto {
    private const val KEY_SIZE = 16

    private val secureRandom = SecureRandom()

    internal val xorKeySeed: ByteArray =
        byteArrayOf(
            0xf0.toByte(),
            0xe5.toByte(),
            0x69.toByte(),
            0xae.toByte(),
            0xbf.toByte(),
            0xdc.toByte(),
            0xbf.toByte(),
            0x8a.toByte(),
            0x1a.toByte(),
            0x45.toByte(),
            0xe8.toByte(),
            0xbe.toByte(),
            0x7d.toByte(),
            0xa6.toByte(),
            0x73.toByte(),
            0xb8.toByte(),
            0xde.toByte(),
            0x8f.toByte(),
            0xe7.toByte(),
            0xc4.toByte(),
            0x45.toByte(),
            0xda.toByte(),
            0x86.toByte(),
            0xc4.toByte(),
            0x9b.toByte(),
            0x64.toByte(),
            0x8b.toByte(),
            0x14.toByte(),
            0x6a.toByte(),
            0xb4.toByte(),
            0xf1.toByte(),
            0xaa.toByte(),
            0x38.toByte(),
            0x01.toByte(),
            0x35.toByte(),
            0x9e.toByte(),
            0x26.toByte(),
            0x69.toByte(),
            0x2c.toByte(),
            0x86.toByte(),
            0x00.toByte(),
            0x6b.toByte(),
            0x4f.toByte(),
            0xa5.toByte(),
            0x36.toByte(),
            0x34.toByte(),
            0x62.toByte(),
            0xa6.toByte(),
            0x2a.toByte(),
            0x96.toByte(),
            0x68.toByte(),
            0x18.toByte(),
            0xf2.toByte(),
            0x4a.toByte(),
            0xfd.toByte(),
            0xbd.toByte(),
            0x6b.toByte(),
            0x97.toByte(),
            0x8f.toByte(),
            0x4d.toByte(),
            0x8f.toByte(),
            0x89.toByte(),
            0x13.toByte(),
            0xb7.toByte(),
            0x6c.toByte(),
            0x8e.toByte(),
            0x93.toByte(),
            0xed.toByte(),
            0x0e.toByte(),
            0x0d.toByte(),
            0x48.toByte(),
            0x3e.toByte(),
            0xd7.toByte(),
            0x2f.toByte(),
            0x88.toByte(),
            0xd8.toByte(),
            0xfe.toByte(),
            0xfe.toByte(),
            0x7e.toByte(),
            0x86.toByte(),
            0x50.toByte(),
            0x95.toByte(),
            0x4f.toByte(),
            0xd1.toByte(),
            0xeb.toByte(),
            0x83.toByte(),
            0x26.toByte(),
            0x34.toByte(),
            0xdb.toByte(),
            0x66.toByte(),
            0x7b.toByte(),
            0x9c.toByte(),
            0x7e.toByte(),
            0x9d.toByte(),
            0x7a.toByte(),
            0x81.toByte(),
            0x32.toByte(),
            0xea.toByte(),
            0xb6.toByte(),
            0x33.toByte(),
            0xde.toByte(),
            0x3a.toByte(),
            0xa9.toByte(),
            0x59.toByte(),
            0x34.toByte(),
            0x66.toByte(),
            0x3b.toByte(),
            0xaa.toByte(),
            0xba.toByte(),
            0x81.toByte(),
            0x60.toByte(),
            0x48.toByte(),
            0xb9.toByte(),
            0xd5.toByte(),
            0x81.toByte(),
            0x9c.toByte(),
            0xf8.toByte(),
            0x6c.toByte(),
            0x84.toByte(),
            0x77.toByte(),
            0xff.toByte(),
            0x54.toByte(),
            0x78.toByte(),
            0x26.toByte(),
            0x5f.toByte(),
            0xbe.toByte(),
            0xe8.toByte(),
            0x1e.toByte(),
            0x36.toByte(),
            0x9f.toByte(),
            0x34.toByte(),
            0x80.toByte(),
            0x5c.toByte(),
            0x45.toByte(),
            0x2c.toByte(),
            0x9b.toByte(),
            0x76.toByte(),
            0xd5.toByte(),
            0x1b.toByte(),
            0x8f.toByte(),
            0xcc.toByte(),
            0xc3.toByte(),
            0xb8.toByte(),
            0xf5.toByte(),
        )

    internal val xorClientKey: ByteArray =
        byteArrayOf(
            0x78.toByte(),
            0x06.toByte(),
            0xad.toByte(),
            0x4c.toByte(),
            0x33.toByte(),
            0x86.toByte(),
            0x5d.toByte(),
            0x18.toByte(),
            0x4c.toByte(),
            0x01.toByte(),
            0x3f.toByte(),
            0x46.toByte(),
        )

    private val rsaN: BigInteger =
        BigInteger(
            "8686980c0f5a24c4b9d43020cd2c22703ff3f450756529058b1cf88f09b86021" +
                "36477198a6e2683149659bd122c33592fdb5ad47944ad1ea4d36c6b172aad633" +
                "8c3bb6ac6227502d010993ac967d1aef00f0c8e038de2e4d3bc2ec368af2e9f1" +
                "0a6f1eda4f7262f136420c07c331b871bf139f74f3010e3c4fe57df3afb71683",
            16,
        )

    private val rsaE: BigInteger = BigInteger("10001", 16)

    private val rsaKeyLength: Int = rsaN.bitLength() / 8

    fun generateKey(): ByteArray = ByteArray(KEY_SIZE).also { secureRandom.nextBytes(it) }

    fun encode(
        input: ByteArray,
        key: ByteArray
    ): String {
        requireValidKey(key)

        val payload = encodePayload(input, key)
        return base64Encode(rsaEncrypt(payload))
    }

    fun decode(
        input: String,
        key: ByteArray
    ): ByteArray {
        requireValidKey(key)

        val encrypted = base64Decode(input) ?: throw IllegalArgumentException("Invalid base64")

        val decrypted = rsaDecrypt(encrypted)
        return decodePayload(decrypted, key)
    }

    internal fun encodePayload(
        input: ByteArray,
        key: ByteArray
    ): ByteArray {
        requireValidKey(key)

        val buf = ByteArray(KEY_SIZE + input.size)
        System.arraycopy(key, 0, buf, 0, KEY_SIZE)
        System.arraycopy(input, 0, buf, KEY_SIZE, input.size)

        xorTransform(buf, KEY_SIZE, buf.size - KEY_SIZE, xorDeriveKey(key, 4))
        reverseBytes(buf, KEY_SIZE, buf.size - KEY_SIZE)
        xorTransform(buf, KEY_SIZE, buf.size - KEY_SIZE, xorClientKey)

        return buf
    }

    internal fun decodePayload(
        decrypted: ByteArray,
        requestKey: ByteArray
    ): ByteArray {
        requireValidKey(requestKey)

        if (decrypted.size <= KEY_SIZE) {
            throw IllegalArgumentException("Invalid payload size=${decrypted.size}")
        }

        val serverKey = decrypted.copyOfRange(0, KEY_SIZE)
        val output = decrypted.copyOfRange(KEY_SIZE, decrypted.size)

        xorTransform(output, 0, output.size, xorDeriveKey(serverKey, 12))
        reverseBytes(output, 0, output.size)
        xorTransform(output, 0, output.size, xorDeriveKey(requestKey, 4))

        return output
    }

    internal fun rsaEncrypt(input: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var offset = 0
        while (offset < input.size) {
            val remain = input.size - offset
            val sliceSize = minOf(rsaKeyLength - 11, remain)
            rsaEncryptSlice(input, offset, sliceSize, out)
            offset += sliceSize
        }
        return out.toByteArray()
    }

    internal fun rsaDecrypt(input: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var offset = 0
        while (offset < input.size) {
            val remain = input.size - offset
            val sliceSize = minOf(rsaKeyLength, remain)
            rsaDecryptSlice(input, offset, sliceSize, out)
            offset += sliceSize
        }
        return out.toByteArray()
    }

    private fun rsaEncryptSlice(
        input: ByteArray,
        offset: Int,
        length: Int,
        out: ByteArrayOutputStream
    ) {
        val padSize = rsaKeyLength - length - 3
        val padData = ByteArray(padSize).also { secureRandom.nextBytes(it) }

        val buf = ByteArray(rsaKeyLength)
        buf[0] = 0
        buf[1] = 2
        for (i in padData.indices) {
            val b = padData[i].toInt() and 0xff
            val nonZero = (b % 0xff) + 0x01
            buf[2 + i] = nonZero.toByte()
        }
        buf[padSize + 2] = 0
        System.arraycopy(input, offset, buf, padSize + 3, length)

        val msg = BigInteger(1, buf)
        var encrypted = msg.modPow(rsaE, rsaN).toByteArray()
        if (encrypted.isNotEmpty() && encrypted[0] == 0.toByte()) {
            encrypted = encrypted.copyOfRange(1, encrypted.size)
        }

        val fillSize = rsaKeyLength - encrypted.size
        if (fillSize > 0) {
            out.write(ByteArray(fillSize))
        }
        out.write(encrypted)
    }

    private fun rsaDecryptSlice(
        input: ByteArray,
        offset: Int,
        length: Int,
        out: ByteArrayOutputStream
    ) {
        val slice = input.copyOfRange(offset, offset + length)
        val msg = BigInteger(1, slice)
        var decrypted = msg.modPow(rsaE, rsaN).toByteArray()
        if (decrypted.isNotEmpty() && decrypted[0] == 0.toByte()) {
            decrypted = decrypted.copyOfRange(1, decrypted.size)
        }

        for (i in decrypted.indices) {
            if (decrypted[i] == 0.toByte() && i != 0) {
                if (i + 1 < decrypted.size) {
                    out.write(decrypted, i + 1, decrypted.size - (i + 1))
                }
                break
            }
        }
    }

    internal fun xorDeriveKey(
        seed: ByteArray,
        size: Int
    ): ByteArray {
        if (seed.size < size) {
            throw IllegalArgumentException("Invalid seed size=${seed.size}, require >= $size")
        }

        val key = ByteArray(size)
        for (i in 0 until size) {
            var v = (seed[i].toInt() and 0xff) + (xorKeySeed[size * i].toInt() and 0xff)
            v = v and 0xff
            v = v xor (xorKeySeed[size * (size - i - 1)].toInt() and 0xff)
            key[i] = v.toByte()
        }
        return key
    }

    internal fun xorTransform(
        data: ByteArray,
        offset: Int,
        length: Int,
        key: ByteArray
    ) {
        if (length <= 0) {
            return
        }
        if (key.isEmpty()) {
            throw IllegalArgumentException("Invalid key size=0")
        }

        val mod = length % 4
        if (mod > 0) {
            for (i in 0 until mod) {
                data[offset + i] = (data[offset + i].toInt() xor key[i % key.size].toInt()).toByte()
            }
        }
        for (i in mod until length) {
            val ki = (i - mod) % key.size
            data[offset + i] = (data[offset + i].toInt() xor key[ki].toInt()).toByte()
        }
    }

    internal fun reverseBytes(
        data: ByteArray,
        offset: Int,
        length: Int
    ) {
        var i = 0
        var j = length - 1
        while (i < j) {
            val a = offset + i
            val b = offset + j
            val tmp = data[a]
            data[a] = data[b]
            data[b] = tmp
            i++
            j--
        }
    }

    private fun requireValidKey(key: ByteArray) {
        require(key.size == KEY_SIZE) { "Invalid key size=${key.size}, require=$KEY_SIZE" }
    }

    private fun base64Encode(bytes: ByteArray): String =
        try {
            val base64 = Class.forName("java.util.Base64")
            val encoder = base64.getMethod("getEncoder").invoke(null)
            encoder.javaClass.getMethod("encodeToString", ByteArray::class.java).invoke(encoder, bytes) as String
        } catch (ignored: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }

    private fun base64Decode(text: String): ByteArray? {
        val input = text.trim()
        if (input.isBlank()) {
            return null
        }

        return try {
            val base64 = Class.forName("java.util.Base64")
            val decoder = base64.getMethod("getDecoder").invoke(null)
            decoder.javaClass.getMethod("decode", String::class.java).invoke(decoder, input) as ByteArray
        } catch (ignored: Throwable) {
            try {
                android.util.Base64.decode(input, android.util.Base64.DEFAULT)
            } catch (e: Throwable) {
                null
            }
        }
    }
}

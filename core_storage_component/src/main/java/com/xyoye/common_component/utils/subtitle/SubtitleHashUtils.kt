package com.xyoye.common_component.utils.subtitle

import com.xyoye.common_component.extension.toHexString
import com.xyoye.common_component.utils.ErrorReportHelper
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

/**
 * Created by xyoye on 2020/11/30.
 */

object SubtitleHashUtils {
    fun getThunderHash(videoPath: String): String? {
        try {
            val messageDigest = MessageDigest.getInstance("SHA1")
            RandomAccessFile(videoPath, "r").use { file ->
                val fileLength = file.length()
                if (fileLength < 0xF000) {
                    val buffer = ByteArray(0xF000)
                    file.seek(0)
                    file.read(buffer)
                    return messageDigest.digest(buffer).toHexString().uppercase(Locale.ROOT)
                }

                val bufferSize = 0x5000
                val positions = longArrayOf(0, fileLength / 3, fileLength - bufferSize)
                for (position in positions) {
                    val buffer = ByteArray(bufferSize)
                    file.seek(position)
                    file.read(buffer)
                    messageDigest.update(buffer)
                }
                return messageDigest.digest().toHexString().uppercase(Locale.ROOT)
            }
        } catch (e: IOException) {
            ErrorReportHelper.postCatchedException(
                e,
                "SubtitleHashUtils.getThunderHash",
                "计算迅雷哈希时IO异常: $videoPath",
            )
        } catch (e: NoSuchAlgorithmException) {
            ErrorReportHelper.postCatchedException(
                e,
                "SubtitleHashUtils.getThunderHash",
                "计算迅雷哈希时算法异常: $videoPath",
            )
        }
        return null
    }

    fun getShooterHash(videoPath: String): String? {
        try {
            val stringBuilder = StringBuilder()
            RandomAccessFile(videoPath, "r").use { file ->
                val fileLength = file.length()
                val positions =
                    longArrayOf(4096, fileLength / 3 * 2, fileLength / 3, fileLength - 8192)
                for (position in positions) {
                    var buffer = ByteArray(4096)
                    if (fileLength < position) {
                        return stringBuilder.toString()
                    }
                    file.seek(position)
                    val realBufferSize: Int = file.read(buffer)
                    buffer = buffer.copyOfRange(0, realBufferSize)
                    val messageDigest = MessageDigest.getInstance("MD5")
                    val byteArray = messageDigest.digest(buffer)
                    stringBuilder.append(byteArray.toHexString())
                    stringBuilder.append(";")
                }

                stringBuilder.deleteCharAt(stringBuilder.length - 1)
                return stringBuilder.toString()
            }
        } catch (e: IOException) {
            ErrorReportHelper.postCatchedException(
                e,
                "SubtitleHashUtils.getShooterHash",
                "计算Shooter哈希时IO异常: $videoPath",
            )
        } catch (e: NoSuchAlgorithmException) {
            ErrorReportHelper.postCatchedException(
                e,
                "SubtitleHashUtils.getShooterHash",
                "计算Shooter哈希时算法异常: $videoPath",
            )
        }
        return null
    }
}

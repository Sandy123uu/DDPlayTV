package com.xyoye.common_component.utils.seven_zip

import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.utils.getFileNameNoExtension
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.sf.sevenzipjbinding.ArchiveFormat
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Created by xyoye on 2020/12/9.
 */

object SevenZipUtils {
    fun isSupportedArchive(fileExtension: String): Boolean {
        if (fileExtension.isEmpty()) return false
        val upperExtension = fileExtension.uppercase(Locale.ROOT)
        return ArchiveFormat.values().any { format ->
            format.methodName.uppercase(Locale.ROOT) == upperExtension
        }
    }

    @Throws(IOException::class)
    suspend fun extractFile(rarFile: File): String? {
        if (!rarFile.exists() || !rarFile.isFile) throw IOException("compress file not found")

        val destDirName: String = getFileNameNoExtension(rarFile.absolutePath)
        val destDir = File(rarFile.parent, destDirName)
        val createdDestDir =
            if (!destDir.exists()) {
                if (destDir.mkdir()) {
                    true
                } else {
                    throw IOException("mkdir output directory failed")
                }
            } else {
                false
            }

        return try {
            extractFileInternal(
                compressFile = rarFile,
                destDir = destDir,
                cleanupOnFailure = createdDestDir,
            )
        } catch (e: CancellationException) {
            if (createdDestDir) {
                destDir.deleteRecursively()
            }
            throw e
        }
    }

    suspend fun extractFile(
        compressFile: File,
        destDir: File
    ): String? {
        return extractFileInternal(
            compressFile = compressFile,
            destDir = destDir,
            cleanupOnFailure = false,
        )
    }

    private suspend fun extractFileInternal(
        compressFile: File,
        destDir: File,
        cleanupOnFailure: Boolean,
    ): String? {
        if (!compressFile.exists() || !compressFile.isFile) throw IOException("compress file not found")
        if (!destDir.exists() || !destDir.isDirectory) throw IOException("Dest directory not found")

        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                var randomAccessFile: RandomAccessFile? = null
                var inArchive: IInArchive? = null
                var extractCallback: ArchiveExtractCallback? = null

                val cleaned = AtomicBoolean(false)

                fun cleanupOutputDir() {
                    if (!cleanupOnFailure) return
                    if (!cleaned.compareAndSet(false, true)) return
                    destDir.deleteRecursively()
                }

                fun closeResources() {
                    try {
                        extractCallback?.close()
                    } catch (_: Throwable) {
                    } finally {
                        extractCallback = null
                    }
                    try {
                        inArchive?.close()
                    } catch (_: Throwable) {
                    } finally {
                        inArchive = null
                    }
                    try {
                        randomAccessFile?.close()
                    } catch (_: Throwable) {
                    } finally {
                        randomAccessFile = null
                    }
                }

                continuation.invokeOnCancellation {
                    cleanupOutputDir()
                    closeResources()
                }

                var result: String? = null
                try {
                    val raf = RandomAccessFile(compressFile, "r").also { randomAccessFile = it }
                    val accessFileInStream = RandomAccessFileInStream(raf)
                    val archive = SevenZip.openInArchive(null, accessFileInStream).also { inArchive = it }
                    extractCallback =
                        ArchiveExtractCallback(archive, destDir) { path ->
                            result = path
                        }

                    archive.extract(null, false, extractCallback)
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    if (continuation.isActive) {
                        ErrorReportHelper.postCatchedException(
                            t,
                            "SevenZipUtils.extractFile",
                            "解压失败: ${compressFile.name}",
                        )
                    }
                    result = null
                } finally {
                    closeResources()
                    if (result == null) {
                        cleanupOutputDir()
                    }
                }

                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
        }
    }
}

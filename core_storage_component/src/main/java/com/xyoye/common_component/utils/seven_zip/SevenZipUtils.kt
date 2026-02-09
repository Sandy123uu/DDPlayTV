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
    ): String? =
        extractFileInternal(
            compressFile = compressFile,
            destDir = destDir,
            cleanupOnFailure = false,
        )

    private suspend fun extractFileInternal(
        compressFile: File,
        destDir: File,
        cleanupOnFailure: Boolean
    ): String? {
        validateExtractInput(compressFile, destDir)
        return withContext(Dispatchers.IO) {
            extractFileCancellable(
                compressFile = compressFile,
                destDir = destDir,
                cleanupOnFailure = cleanupOnFailure,
            )
        }
    }

    private suspend fun extractFileCancellable(
        compressFile: File,
        destDir: File,
        cleanupOnFailure: Boolean
    ): String? =
        suspendCancellableCoroutine { continuation ->
            var randomAccessFile: RandomAccessFile? = null
            var inArchive: IInArchive? = null
            var extractCallback: ArchiveExtractCallback? = null
            val cleaned = AtomicBoolean(false)

            continuation.invokeOnCancellation {
                cleanupOutputDirIfNeeded(cleanupOnFailure, destDir, cleaned)
                closeExtractResources(extractCallback, inArchive, randomAccessFile)
            }

            val result =
                runCatching {
                    val raf = RandomAccessFile(compressFile, "r").also { randomAccessFile = it }
                    val accessFileInStream = RandomAccessFileInStream(raf)
                    val archive = SevenZip.openInArchive(null, accessFileInStream).also { inArchive = it }
                    var extractPath: String? = null
                    extractCallback =
                        ArchiveExtractCallback(archive, destDir) { path ->
                            extractPath = path
                        }
                    archive.extract(null, false, extractCallback)
                    extractPath
                }.getOrElse { throwable ->
                    if (throwable is CancellationException) {
                        throw throwable
                    }
                    if (continuation.isActive) {
                        ErrorReportHelper.postCatchedException(
                            throwable,
                            "SevenZipUtils.extractFile",
                            "解压失败: ${compressFile.name}",
                        )
                    }
                    null
                }

            closeExtractResources(extractCallback, inArchive, randomAccessFile)
            if (result == null) {
                cleanupOutputDirIfNeeded(cleanupOnFailure, destDir, cleaned)
            }
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }

    private fun validateExtractInput(
        compressFile: File,
        destDir: File
    ) {
        if (!compressFile.exists() || !compressFile.isFile) throw IOException("compress file not found")
        if (!destDir.exists() || !destDir.isDirectory) throw IOException("Dest directory not found")
    }

    private fun cleanupOutputDirIfNeeded(
        cleanupOnFailure: Boolean,
        destDir: File,
        cleaned: AtomicBoolean
    ) {
        if (!cleanupOnFailure) return
        if (!cleaned.compareAndSet(false, true)) return
        destDir.deleteRecursively()
    }

    private fun closeExtractResources(
        extractCallback: ArchiveExtractCallback?,
        inArchive: IInArchive?,
        randomAccessFile: RandomAccessFile?
    ) {
        runCatching { extractCallback?.close() }
        runCatching { inArchive?.close() }
        runCatching { randomAccessFile?.close() }
    }
}

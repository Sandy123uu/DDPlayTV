package com.xyoye.common_component.log.http

import java.io.BufferedOutputStream
import java.io.File
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal object HttpLogArchiveExporter {
    private const val SEGMENT_SUFFIX = ".jsonl"
    private const val PIPE_BUFFER_SIZE = 64 * 1024

    fun open(logsDir: File): HttpLogDownloadPayload {
        val exportedAtMs = System.currentTimeMillis()
        val fileName = buildArchiveName(exportedAtMs)
        val inputStream = PipedInputStream(PIPE_BUFFER_SIZE)
        val outputStream = PipedOutputStream(inputStream)

        Thread(
            {
                writeArchive(
                    logsDir = logsDir,
                    exportedAtMs = exportedAtMs,
                    outputStream = outputStream,
                )
            },
            "HttpLogArchiveWriter",
        ).apply { isDaemon = true }.start()

        return HttpLogDownloadPayload(
            inputStream = inputStream,
            fileName = fileName,
        )
    }

    fun openLatestSegment(logsDir: File): HttpLogDownloadPayload? {
        val latestFile = listSegmentFiles(logsDir).lastOrNull() ?: return null
        return HttpLogDownloadPayload(
            inputStream = latestFile.inputStream().buffered(),
            fileName = latestFile.name,
        )
    }

    private fun writeArchive(
        logsDir: File,
        exportedAtMs: Long,
        outputStream: PipedOutputStream,
    ) {
        outputStream.use { piped ->
            ZipOutputStream(BufferedOutputStream(piped)).use { zipOutput ->
                val files = listSegmentFiles(logsDir)
                val manifest = buildManifest(exportedAtMs, files)
                zipOutput.writeEntry(
                    entryName = "manifest.json",
                    content = manifest.toByteArray(Charsets.UTF_8),
                    timestampMs = exportedAtMs,
                )

                files.forEach { file ->
                    zipOutput.putNextEntry(
                        ZipEntry(file.name).apply {
                            time = file.lastModified().takeIf { it > 0L } ?: exportedAtMs
                        },
                    )
                    file.inputStream().buffered().use { input ->
                        input.copyTo(zipOutput)
                    }
                    zipOutput.closeEntry()
                }
            }
        }
    }

    private fun listSegmentFiles(logsDir: File): List<File> {
        if (!logsDir.exists() || !logsDir.isDirectory) {
            return emptyList()
        }
        return logsDir
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.lowercase(Locale.US).endsWith(SEGMENT_SUFFIX) }
            .sortedWith(compareBy<File> { it.lastModified() }.thenBy { it.name })
    }

    private fun buildArchiveName(exportedAtMs: Long): String {
        val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val timestamp = formatter.format(Date(exportedAtMs))
        return "ddplaytv-logs-$timestamp.zip"
    }

    private fun buildManifest(
        exportedAtMs: Long,
        files: List<File>,
    ): String =
        buildString {
            append("{\"exportedAtMs\":").append(exportedAtMs)
            append(",\"fileCount\":").append(files.size)
            append(",\"files\":[")
            files.forEachIndexed { index, file ->
                if (index > 0) append(',')
                append("{\"name\":\"")
                append(escapeJson(file.name))
                append("\",\"sizeBytes\":")
                append(file.length().coerceAtLeast(0L))
                append(",\"lastModifiedMs\":")
                append(file.lastModified().coerceAtLeast(0L))
                append('}')
            }
            append("]}")
        }

    private fun ZipOutputStream.writeEntry(
        entryName: String,
        content: ByteArray,
        timestampMs: Long,
    ) {
        putNextEntry(
            ZipEntry(entryName).apply {
                time = timestampMs
            },
        )
        write(content)
        closeEntry()
    }

    private fun escapeJson(value: String): String =
        buildString(value.length + 8) {
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
}

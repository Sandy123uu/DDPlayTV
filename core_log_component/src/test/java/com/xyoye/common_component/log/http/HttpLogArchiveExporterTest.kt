package com.xyoye.common_component.log.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files

@RunWith(JUnit4::class)
class HttpLogArchiveExporterTest {
    @Test
    fun openLatestSegmentReturnsNewestSegmentFile() {
        val logsDir = Files.createTempDirectory("http-log-exporter").toFile()
        try {
            val older = logsDir.resolve("seg_1.jsonl")
            older.writeText("""{"id":1}""")
            older.setLastModified(1_000L)

            val newer = logsDir.resolve("seg_2.jsonl")
            newer.writeText("""{"id":2}""")
            newer.setLastModified(2_000L)

            logsDir.resolve("ignore.txt").writeText("ignore")

            val payload = HttpLogArchiveExporter.openLatestSegment(logsDir)
            requireNotNull(payload)

            assertEquals("seg_2.jsonl", payload.fileName)
            val text = payload.inputStream.bufferedReader().use { it.readText() }
            assertEquals("""{"id":2}""", text)
        } finally {
            logsDir.deleteRecursively()
        }
    }

    @Test
    fun openLatestSegmentReturnsNullWhenNoSegmentExists() {
        val logsDir = Files.createTempDirectory("http-log-exporter-empty").toFile()
        try {
            assertNull(HttpLogArchiveExporter.openLatestSegment(logsDir))
        } finally {
            logsDir.deleteRecursively()
        }
    }
}

package com.xyoye.common_component.log.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun openSegmentAtReturnsMatchingSegmentByTimestamp() {
        val logsDir = Files.createTempDirectory("http-log-exporter-segment-at").toFile()
        try {
            logsDir.resolve("seg_1000.jsonl").writeText("""{"id":1}""")
            logsDir.resolve("seg_2000.jsonl").writeText("""{"id":2}""")
            logsDir.resolve("seg_3000.jsonl").writeText("""{"id":3}""")

            val midPayload = HttpLogArchiveExporter.openSegmentAt(logsDir, 2_500L)
            requireNotNull(midPayload)
            assertEquals("seg_2000.jsonl", midPayload.fileName)

            val tailPayload = HttpLogArchiveExporter.openSegmentAt(logsDir, 3_900L)
            requireNotNull(tailPayload)
            assertEquals("seg_3000.jsonl", tailPayload.fileName)
        } finally {
            logsDir.deleteRecursively()
        }
    }

    @Test
    fun openSegmentAtReturnsNullWhenTimestampIsBeforeFirstSegment() {
        val logsDir = Files.createTempDirectory("http-log-exporter-segment-at-empty").toFile()
        try {
            logsDir.resolve("seg_1000.jsonl").writeText("""{"id":1}""")
            assertNull(HttpLogArchiveExporter.openSegmentAt(logsDir, 999L))
        } finally {
            logsDir.deleteRecursively()
        }
    }

    @Test
    fun openSegmentAtFallsBackToLastModifiedWhenNameCannotBeParsed() {
        val logsDir = Files.createTempDirectory("http-log-exporter-segment-at-fallback").toFile()
        try {
            val fallback = logsDir.resolve("legacy.jsonl")
            fallback.writeText("""{"id":99}""")
            fallback.setLastModified(5_000L)

            val newer = logsDir.resolve("seg_10000.jsonl")
            newer.writeText("""{"id":100}""")
            newer.setLastModified(10_000L)

            val payload = HttpLogArchiveExporter.openSegmentAt(logsDir, 5_500L)
            requireNotNull(payload)
            assertEquals("legacy.jsonl", payload.fileName)
        } finally {
            logsDir.deleteRecursively()
        }
    }

    @Test
    fun openSegmentAtReturnsNullWhenNoSegmentExists() {
        val logsDir = Files.createTempDirectory("http-log-exporter-segment-at-none").toFile()
        try {
            assertNull(HttpLogArchiveExporter.openSegmentAt(logsDir, 1_000L))
        } finally {
            logsDir.deleteRecursively()
        }
    }

    @Test
    fun listSegmentsReturnsDescendingTimelineWithSummaryFields() {
        val logsDir = Files.createTempDirectory("http-log-exporter-list-segments").toFile()
        try {
            val first = logsDir.resolve("seg_1000.jsonl")
            first.writeText("""{"id":1}""")
            first.setLastModified(1_100L)

            val second = logsDir.resolve("legacy.jsonl")
            second.writeText("""{"id":2}""")
            second.setLastModified(1_500L)

            val latest = logsDir.resolve("seg_2000.jsonl")
            latest.writeText("""{"id":3}""")
            latest.setLastModified(2_100L)

            val segments = HttpLogArchiveExporter.listSegments(logsDir)
            assertEquals(3, segments.size)

            val firstItem = segments[0]
            assertEquals("seg_2000.jsonl", firstItem.fileName)
            assertEquals(2_000L, firstItem.startMs)
            assertNull(firstItem.endMs)
            assertTrue(firstItem.isLatest)

            val middleItem = segments[1]
            assertEquals("legacy.jsonl", middleItem.fileName)
            assertEquals(1_500L, middleItem.startMs)
            assertEquals(1_999L, middleItem.endMs)
            assertTrue(!middleItem.isLatest)

            val lastItem = segments[2]
            assertEquals("seg_1000.jsonl", lastItem.fileName)
            assertEquals(1_000L, lastItem.startMs)
            assertEquals(1_499L, lastItem.endMs)
            assertTrue(!lastItem.isLatest)
        } finally {
            logsDir.deleteRecursively()
        }
    }

    @Test
    fun listSegmentsReturnsEmptyWhenNoSegmentExists() {
        val logsDir = Files.createTempDirectory("http-log-exporter-list-segments-empty").toFile()
        try {
            val segments = HttpLogArchiveExporter.listSegments(logsDir)
            assertTrue(segments.isEmpty())
        } finally {
            logsDir.deleteRecursively()
        }
    }
}

package com.xyoye.common_component.utils.seven_zip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class SevenZipUtilsInstrumentedTest {
    private lateinit var context: Context
    private lateinit var testRoot: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testRoot = File(context.cacheDir, "seven_zip_instrumented_test")
        if (testRoot.exists()) {
            testRoot.deleteRecursively()
        }
        testRoot.mkdirs()
    }

    @After
    fun tearDown() {
        if (testRoot.exists()) {
            testRoot.deleteRecursively()
        }
    }

    @Test
    fun extractZipAnd7zFixtures_matchExpectedHashAndSize() {
        runBlocking {
            assertArchiveExtracted("seven_zip/chunked_payload.zip")
            assertArchiveExtracted("seven_zip/chunked_payload.7z")
        }
    }

    @Test
    fun corruptedArchive_returnsNullAndCleansOutputDir() {
        runBlocking {
            val corruptedArchive = File(testRoot, "corrupted_payload.7z")
            corruptedArchive.writeBytes(ByteArray(4096) { index -> (index * 31).toByte() })

            val extractPath = SevenZipUtils.extractFile(corruptedArchive)

            assertNull(extractPath)
            assertFalse(File(testRoot, "corrupted_payload").exists())
        }
    }

    @Test
    fun cancelExtraction_releasesResourcesAndAllowsCleanup() {
        runBlocking {
            val largeArchive = File(testRoot, "cancel_large.zip")
            createLargeZipArchive(largeArchive, entryCount = 48, entrySizeBytes = 4 * 1024 * 1024)

            val extraction =
                async(Dispatchers.Default) {
                    SevenZipUtils.extractFile(largeArchive)
                }

            delay(20)
            extraction.cancel()
            val cancelResult = runCatching { extraction.await() }
            assertTrue(cancelResult.isFailure)
            assertTrue(cancelResult.exceptionOrNull() is CancellationException)

            val outputDir = File(testRoot, "cancel_large")
            if (outputDir.exists()) {
                assertTrue(outputDir.deleteRecursively())
            }

            assertTrue(largeArchive.delete())
        }
    }

    private suspend fun assertArchiveExtracted(assetPath: String) {
        val extension = assetPath.substringAfterLast('.')
        val archive = copyAssetToFile(assetPath, "fixture_${System.nanoTime()}.$extension")
        val extractPath = SevenZipUtils.extractFile(archive)

        assertNotNull(extractPath)
        val outputDir = File(extractPath!!)
        assertTrue(outputDir.exists())

        EXPECTED_FILES.forEach { (fileName, expected) ->
            val outputFile = File(outputDir, fileName)
            assertTrue(outputFile.exists())
            assertEquals(expected.size, outputFile.length())
            assertEquals(expected.sha256, sha256(outputFile))
        }
    }

    private fun copyAssetToFile(
        assetPath: String,
        outputName: String
    ): File {
        val output = File(testRoot, outputName)
        context.assets.open(assetPath).use { input ->
            output.outputStream().use { stream ->
                input.copyTo(stream)
            }
        }
        return output
    }

    private fun createLargeZipArchive(
        output: File,
        entryCount: Int,
        entrySizeBytes: Int
    ) {
        val buffer = ByteArray(32 * 1024) { index -> ((index * 37 + 11) and 0xFF).toByte() }
        ZipOutputStream(FileOutputStream(output)).use { zipOutputStream ->
            zipOutputStream.setLevel(0)
            repeat(entryCount) { index ->
                val entry = ZipEntry("large_payload_$index.bin")
                zipOutputStream.putNextEntry(entry)
                var remain = entrySizeBytes
                while (remain > 0) {
                    val writeSize = minOf(remain, buffer.size)
                    zipOutputStream.write(buffer, 0, writeSize)
                    remain -= writeSize
                }
                zipOutputStream.closeEntry()
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    private data class ExpectedFile(
        val size: Long,
        val sha256: String
    )

    private companion object {
        private val EXPECTED_FILES: Map<String, ExpectedFile> =
            mapOf(
                "payload_chunked.bin" to
                    ExpectedFile(
                        size = 1_048_576,
                        sha256 = "1612586a56503d400b5796768f9ce3bde548b001d80a07f2d5bb0a45c98fec09",
                    ),
                "alpha.txt" to
                    ExpectedFile(
                        size = 68,
                        sha256 = "f63deadcaa4e1fc1891530ee680b23c24829f944cf6a09a82c60a7a24541f246",
                    ),
                "beta.json" to
                    ExpectedFile(
                        size = 74,
                        sha256 = "c81148c0086138b35d3bd3894ebc241f41a659a8a154e26c6ba65c3dae3285ed",
                    ),
            )
    }
}

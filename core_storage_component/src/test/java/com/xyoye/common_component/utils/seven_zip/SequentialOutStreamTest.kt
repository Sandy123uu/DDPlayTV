package com.xyoye.common_component.utils.seven_zip

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SequentialOutStreamTest {
    @Test
    fun write_appendsAcrossMultipleCalls() {
        val destDir = Files.createTempDirectory("SequentialOutStreamTest").toFile()
        val stream = SequentialOutStream(destDir, "out.txt")

        val first = "hello".toByteArray()
        val second = "world".toByteArray()

        assertEquals(first.size, stream.write(first))
        assertEquals(second.size, stream.write(second))
        stream.close()

        val bytes = File(destDir, "out.txt").readBytes()
        assertArrayEquals(first + second, bytes)
    }
}

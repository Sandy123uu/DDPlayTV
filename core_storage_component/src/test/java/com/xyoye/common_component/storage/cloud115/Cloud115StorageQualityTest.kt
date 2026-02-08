package com.xyoye.common_component.storage.cloud115

import com.xyoye.common_component.storage.impl.Cloud115Storage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Cloud115StorageQualityTest {
    @Test
    fun validateSearchKeywordTrimsInput() {
        val keyword = Cloud115Storage.validateSearchKeyword("  one-piece  ")
        assertEquals("one-piece", keyword)
    }

    @Test
    fun validateSearchKeywordRejectsTooLongValue() {
        val failure = runCatching { Cloud115Storage.validateSearchKeyword("x".repeat(31)) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message?.contains("关键词过长") == true)
    }

    @Test
    fun requireNonBlankIdentifierReturnsTrimmedValue() {
        val identifier = Cloud115Storage.requireNonBlankIdentifier("  fid-123  ", "invalid")
        assertEquals("fid-123", identifier)
    }

    @Test
    fun requireNonBlankIdentifierRejectsBlankValue() {
        val failure = runCatching {
            Cloud115Storage.requireNonBlankIdentifier("   ", "无效文件ID")
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals("无效文件ID", failure?.message)
    }
}

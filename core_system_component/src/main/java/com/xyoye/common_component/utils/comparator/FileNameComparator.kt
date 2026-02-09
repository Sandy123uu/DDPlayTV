package com.xyoye.common_component.utils.comparator

import java.text.CollationKey
import java.text.Collator
import java.util.Comparator
import kotlin.math.min

/**
 * Created by xyoye on 2023/1/23.
 */

private val COLLATION_SENTINEL = byteArrayOf(1, 1, 1)

class FileNameComparator<T>(
    private val getName: (T) -> String,
    private val isDirectory: (T) -> Boolean,
    private val asc: Boolean = true,
    private val directoryFirst: Boolean = true
) : Comparator<T> {
    override fun compare(
        o1: T?,
        o2: T?
    ): Int {
        if (o1 == null) {
            return if (asc) -1 else 1
        }
        if (o2 == null) {
            return if (asc) 1 else -1
        }

        val isDirectory1 = directory(o1, o2, true)
        val isDirectory2 = directory(o1, o2, false)

        return when {
            isDirectory1 == isDirectory2 -> compareFileName(o1, o2)
            isDirectory1 -> -1
            else -> 1
        }
    }

    private fun compareFileName(
        o1: T,
        o2: T
    ): Int {
        val key1 = nameKey(o1, o2, true)
        val key2 = nameKey(o1, o2, false)
        return key1.compareTo(key2)
    }

    private fun nameKey(
        o1: T,
        o2: T,
        first: Boolean
    ): CollationKey =
        if (first) {
            if (asc) o1 else o2
        } else {
            if (asc) o2 else o1
        }.run(getName).run {
            Collator.getInstance().getCollateKey(this)
        }

    private fun directory(
        o1: T,
        o2: T,
        first: Boolean
    ): Boolean =
        if (first) {
            if (directoryFirst) o1 else o2
        } else {
            if (directoryFirst) o2 else o1
        }.run(isDirectory)
}

private class ByteArrayCollationKey(
    @Suppress("CanBeParameter")
    private val source: String,
    private val bytes: ByteArray
) : CollationKey(source) {
    override fun compareTo(other: CollationKey): Int {
        other as ByteArrayCollationKey
        return bytes.unsignedCompareTo(other.bytes)
    }

    override fun toByteArray(): ByteArray = bytes.copyOf()
}

// @see https://github.com/GNOME/glib/blob/mainline/glib/gunicollate.c
//      g_utf8_collate_key_for_filename()
fun Collator.getCollateKey(source: String): CollationKey {
    val result = ByteStringBuilder()
    val suffix = ByteStringBuilder()
    var previousIndex = 0
    var index = 0
    val endIndex = source.length

    while (index < endIndex) {
        when {
            source[index] == '.' -> {
                previousIndex = appendDotSegment(source, previousIndex, index, result)
                index += 1
            }
            source[index].isAsciiDigit() -> {
                val digitSegmentResult = appendDigitSegment(source, previousIndex, index, result, suffix)
                previousIndex = digitSegmentResult.nextPreviousIndex
                index = digitSegmentResult.nextIndex
            }
            else -> {
                index += 1
            }
        }
    }

    appendPlainSegment(source, previousIndex, endIndex, result)
    result.append(suffix.toByteString())
    return ByteArrayCollationKey(source, result.toByteString().borrowBytes())
}

private data class DigitSegmentResult(
    val nextIndex: Int,
    val nextPreviousIndex: Int
)

private fun Collator.appendPlainSegment(
    source: String,
    startIndex: Int,
    endIndex: Int,
    result: ByteStringBuilder
) {
    if (startIndex >= endIndex) return
    val collationKey = getCollationKey(source.substring(startIndex, endIndex))
    result.append(collationKey.toByteArray())
}

private fun Collator.appendDotSegment(
    source: String,
    previousIndex: Int,
    index: Int,
    result: ByteStringBuilder
): Int {
    appendPlainSegment(source, previousIndex, index, result)
    result.append(COLLATION_SENTINEL).append(1)
    return index + 1
}

private fun Collator.appendDigitSegment(
    source: String,
    previousIndex: Int,
    startIndex: Int,
    result: ByteStringBuilder,
    suffix: ByteStringBuilder
): DigitSegmentResult {
    appendPlainSegment(source, previousIndex, startIndex, result)
    result.append(COLLATION_SENTINEL).append(2)

    val endIndex = source.length
    var scanIndex = startIndex
    var segmentStart = startIndex
    var leadingZeros: Int
    var digits: Int

    if (source[scanIndex] == '0') {
        leadingZeros = 1
        digits = 0
    } else {
        leadingZeros = 0
        digits = 1
    }

    while (++scanIndex < endIndex) {
        when {
            source[scanIndex] == '0' && digits == 0 -> {
                leadingZeros += 1
            }
            source[scanIndex].isAsciiDigit() -> {
                digits += 1
            }
            else -> {
                if (digits == 0) {
                    digits += 1
                    leadingZeros -= 1
                }
                break
            }
        }
    }

    while (digits > 1) {
        result.append(':'.code.toByte())
        digits -= 1
    }

    if (leadingZeros > 0) {
        suffix.append(leadingZeros.toByte())
        segmentStart += leadingZeros
    }

    result.append(source.substring(segmentStart, scanIndex).toByteString())
    return DigitSegmentResult(nextIndex = scanIndex, nextPreviousIndex = scanIndex)
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

private fun ByteArray.unsignedCompareTo(other: ByteArray): Int {
    val size = size
    val otherSize = other.size
    for (index in 0 until min(size, otherSize)) {
        val byte = this[index].toInt() and 0xFF
        val otherByte = other[index].toInt() and 0xFF
        if (byte < otherByte) {
            return -1
        } else if (byte > otherByte) {
            return 1
        }
    }
    return size - otherSize
}

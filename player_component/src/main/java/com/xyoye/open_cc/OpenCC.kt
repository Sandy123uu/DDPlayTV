package com.xyoye.open_cc

import android.os.Build
import java.io.File

/**
 * Created by xyoye on 2023/5/27
 */

object OpenCC {
    @Volatile
    private var nativeLoaded = false

    @Volatile
    private var nativeLoadError: Throwable? = null

    private val nativeLoadLock = Any()

    private val nativeConvertLock = Any()

    private external fun convert(
        text: String,
        configJsonPath: String
    ): String

    fun convertSC(text: String): String {
        if (text.isEmpty()) return text

        // Prefer ICU on API 24+ to avoid native OpenCC aborts caused by uncaught C++ exceptions.
        convertWithIcuOrNull(text, toSimplified = true)?.let { return it }

        if (OpenCCFile.isT2sReady().not()) return text
        return convertWithConfigFile(text, OpenCCFile.t2s)
    }

    fun convertTC(text: String): String {
        if (text.isEmpty()) return text

        // Prefer ICU on API 24+ to avoid native OpenCC aborts caused by uncaught C++ exceptions.
        convertWithIcuOrNull(text, toSimplified = false)?.let { return it }

        if (OpenCCFile.isS2tReady().not()) return text
        return convertWithConfigFile(text, OpenCCFile.s2t)
    }

    private fun convertWithIcuOrNull(
        text: String,
        toSimplified: Boolean
    ): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return null
        }

        return try {
            synchronized(IcuConverter) {
                IcuConverter.convert(text, toSimplified)
            }
        } catch (t: Throwable) {
            com.xyoye.common_component.utils.ErrorReportHelper.postCatchedException(
                t,
                "OpenCCIcu",
                "Failed to convert text with ICU, toSimplified=$toSimplified",
            )
            null
        }
    }

    private object IcuConverter {
        private const val ICU_CLASS_NAME = "android.icu.text.Transliterator"
        private const val ID_TRADITIONAL_TO_SIMPLIFIED = "Traditional-Simplified"
        private const val ID_SIMPLIFIED_TO_TRADITIONAL = "Simplified-Traditional"

        @Volatile
        private var transliteratorClass: Class<*>? = null

        @Volatile
        private var getInstanceMethod: java.lang.reflect.Method? = null

        @Volatile
        private var transliterateMethod: java.lang.reflect.Method? = null

        @Volatile
        private var traditionalToSimplified: Any? = null

        @Volatile
        private var simplifiedToTraditional: Any? = null

        private fun ensureInitialized() {
            if (transliteratorClass != null) return

            synchronized(this) {
                if (transliteratorClass != null) return

                val clazz = Class.forName(ICU_CLASS_NAME)
                transliteratorClass = clazz
                getInstanceMethod = clazz.getMethod("getInstance", String::class.java)
                transliterateMethod = clazz.getMethod("transliterate", String::class.java)
            }
        }

        private fun ensureInstances() {
            if (traditionalToSimplified != null && simplifiedToTraditional != null) return

            synchronized(this) {
                if (traditionalToSimplified != null && simplifiedToTraditional != null) return

                ensureInitialized()
                val getInstance =
                    getInstanceMethod
                        ?: throw IllegalStateException("ICU Transliterator#getInstance not initialized")

                if (traditionalToSimplified == null) {
                    traditionalToSimplified = getInstance.invoke(null, ID_TRADITIONAL_TO_SIMPLIFIED)
                }
                if (simplifiedToTraditional == null) {
                    simplifiedToTraditional = getInstance.invoke(null, ID_SIMPLIFIED_TO_TRADITIONAL)
                }
            }
        }

        fun convert(
            text: String,
            toSimplified: Boolean
        ): String {
            ensureInstances()

            val instance = if (toSimplified) traditionalToSimplified else simplifiedToTraditional
            val transliterate =
                transliterateMethod ?: throw IllegalStateException("ICU Transliterator#transliterate not initialized")

            val result = transliterate.invoke(instance, text)
            return result as? String ?: text
        }
    }

    private fun convertWithConfigFile(
        text: String,
        config: File
    ): String =
        try {
            convertCompat(text, config.absolutePath)
        } catch (t: Throwable) {
            com.xyoye.common_component.utils.ErrorReportHelper.postCatchedException(
                t,
                "OpenCC",
                "Failed to convert text with config: ${config.absolutePath}",
            )
            text
        }

    /**
     * `libopen_cc.so` uses JNI string conversion and expects valid standard UTF-8 input.
     *
     * Some characters (e.g. `\u0000` or non-BMP code points like emoji) may be encoded as
     * *modified UTF-8* across JNI, which is invalid for OpenCC and can trigger a native
     * abort (SIGABRT) due to an uncaught C++ exception.
     *
     * To avoid crashing, we split the text and only feed "safe" segments to OpenCC, while
     * keeping other characters unchanged.
     */
    private fun convertCompat(
        text: String,
        configJsonPath: String
    ): String {
        if (text.isEmpty()) {
            return text
        }

        if (containsJniModifiedUtf8IncompatibleChars(text).not()) {
            return convertLocked(text, configJsonPath)
        }

        val output = StringBuilder(text.length)
        val safeSegment = StringBuilder()

        fun flushSafeSegment() {
            if (safeSegment.isEmpty()) return

            val segment = safeSegment.toString()
            safeSegment.setLength(0)
            output.append(convertSegmentOrFallback(segment, configJsonPath))
        }

        var index = 0
        while (index < text.length) {
            val char = text[index]

            if (char == '\u0000') {
                flushSafeSegment()
                output.append(char)
                index++
                continue
            }

            if (Character.isHighSurrogate(char)) {
                flushSafeSegment()

                if (index + 1 < text.length && Character.isLowSurrogate(text[index + 1])) {
                    val codePoint = Character.toCodePoint(char, text[index + 1])
                    output.appendCodePoint(codePoint)
                    index += 2
                } else {
                    output.append(char)
                    index++
                }
                continue
            }

            if (Character.isLowSurrogate(char)) {
                flushSafeSegment()
                output.append(char)
                index++
                continue
            }

            safeSegment.append(char)
            index++
        }

        flushSafeSegment()
        return output.toString()
    }

    private fun containsJniModifiedUtf8IncompatibleChars(text: String): Boolean {
        for (i in text.indices) {
            val c = text[i]
            if (c == '\u0000' || Character.isSurrogate(c)) {
                return true
            }
        }
        return false
    }

    private fun convertLocked(
        text: String,
        configJsonPath: String
    ): String =
        synchronized(nativeConvertLock) {
            ensureNativeLoaded()
            convert(text, configJsonPath)
        }

    private fun ensureNativeLoaded() {
        nativeLoadError?.let { throw it }
        if (nativeLoaded) return

        synchronized(nativeLoadLock) {
            nativeLoadError?.let { throw it }
            if (nativeLoaded) return

            try {
                System.loadLibrary("open_cc")
                nativeLoaded = true
            } catch (t: Throwable) {
                nativeLoadError = t
                throw t
            }
        }
    }

    private fun convertSegmentOrFallback(
        text: String,
        configJsonPath: String
    ): String =
        try {
            convertLocked(text, configJsonPath)
        } catch (t: Throwable) {
            com.xyoye.common_component.utils.ErrorReportHelper.postCatchedException(
                t,
                "OpenCC",
                "Failed to convert text segment with config: $configJsonPath",
            )
            text
        }
}

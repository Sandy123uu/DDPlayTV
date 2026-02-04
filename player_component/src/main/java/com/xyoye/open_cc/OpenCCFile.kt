package com.xyoye.open_cc

import android.content.Context
import com.xyoye.common_component.utils.PathHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by xyoye on 2023/5/27
 */

object OpenCCFile {
    private val OPEN_CC_ASSET_FILES =
        listOf(
            "STCharacters.ocd2",
            "STPhrases.ocd2",
            "TSCharacters.ocd2",
            "TSPhrases.ocd2",
            "s2t.json",
            "t2s.json",
        )

    // 简转繁配置文件
    val s2t: File = File(PathHelper.getOpenCCDirectory(), "s2t.json")

    // 繁转简配置文件
    val t2s: File = File(PathHelper.getOpenCCDirectory(), "t2s.json")

    private val stCharacters: File = File(PathHelper.getOpenCCDirectory(), "STCharacters.ocd2")
    private val stPhrases: File = File(PathHelper.getOpenCCDirectory(), "STPhrases.ocd2")
    private val tsCharacters: File = File(PathHelper.getOpenCCDirectory(), "TSCharacters.ocd2")
    private val tsPhrases: File = File(PathHelper.getOpenCCDirectory(), "TSPhrases.ocd2")

    // assets中open_cc文件夹名称
    private const val OPEN_CC_ASSETS_DIR = "open_cc"

    private val requiredFiles: List<File> =
        listOf(
            stCharacters,
            stPhrases,
            tsCharacters,
            tsPhrases,
            s2t,
            t2s,
        )

    private val requiredFilesT2s: List<File> =
        listOf(
            tsCharacters,
            tsPhrases,
            t2s,
        )

    private val requiredFilesS2t: List<File> =
        listOf(
            stCharacters,
            stPhrases,
            s2t,
        )

    fun isT2sReady(): Boolean =
        requiredFilesT2s.all {
            it.exists() && it.length() > 0L
        }

    fun isS2tReady(): Boolean =
        requiredFilesS2t.all {
            it.exists() && it.length() > 0L
        }

    fun isReady(): Boolean =
        requiredFiles.all {
            it.exists() && it.length() > 0L
        }

    @Volatile
    var init = false

    /**
     * 初始化open_cc相关文件
     */
    fun init(context: Context) {
        try {
            val openCCDir = PathHelper.getOpenCCDirectory()
            for (fileName in OPEN_CC_ASSET_FILES) {
                val internalFile = File(openCCDir, fileName)
                if (internalFile.exists() && internalFile.length() > 0L) continue

                val assetsFilePath = "$OPEN_CC_ASSETS_DIR/$fileName"
                copyFileFromAssetsAtomic(context, assetsFilePath, internalFile)
            }
        } catch (e: Exception) {
            com.xyoye.common_component.utils.ErrorReportHelper.postCatchedException(
                e,
                "OpenCCFile",
                "Failed to initialize OpenCC files",
            )
        } finally {
            init = isReady()
        }
    }

    /**
     * 从assets目录中复制文件到本地
     */
    private fun copyFileFromAssetsAtomic(
        context: Context,
        assetsFilePath: String,
        destFile: File
    ) {
        val parent = destFile.parentFile ?: throw IOException("Invalid dest file: ${destFile.absolutePath}")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Failed to create directory: ${parent.absolutePath}")
        }

        val tmpFile = File(parent, "${destFile.name}.tmp")
        try {
            if (tmpFile.exists() && !tmpFile.delete()) {
                throw IOException("Failed to delete tmp file: ${tmpFile.absolutePath}")
            }

            context.assets.open(assetsFilePath).use { input ->
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            if (tmpFile.length() <= 0L) {
                throw IOException("Copied file is empty: $assetsFilePath")
            }

            if (destFile.exists() && !destFile.delete()) {
                throw IOException("Failed to delete existing file: ${destFile.absolutePath}")
            }
            if (!tmpFile.renameTo(destFile)) {
                FileOutputStream(destFile).use { output ->
                    tmpFile.inputStream().use { input ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                tmpFile.delete()
            }
        } catch (e: Exception) {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            throw e
        }
    }
}

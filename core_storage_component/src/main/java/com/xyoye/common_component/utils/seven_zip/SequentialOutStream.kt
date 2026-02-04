package com.xyoye.common_component.utils.seven_zip

import com.xyoye.common_component.utils.ErrorReportHelper
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.SevenZipException
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SequentialOutStream(
    private val destDir: File,
    private var fileName: String
) : ISequentialOutStream,
    Closeable {
    private var outFile: File? = null
    private var outputStream: FileOutputStream? = null

    private fun ensureOutputStream(): FileOutputStream {
        outputStream?.let { return it }

        if (!destDir.exists() || !destDir.isDirectory) {
            throw SevenZipException("out put directory error")
        }
        if (fileName.isEmpty()) {
            fileName = destDir.name.toString() + "_" + System.currentTimeMillis()
        }

        val file = File(destDir, fileName)
        outFile = file
        return try {
            FileOutputStream(file).also { outputStream = it }
        } catch (e: IOException) {
            ErrorReportHelper.postCatchedException(
                e,
                "SequentialOutStream.ensureOutputStream",
                "创建7z输出文件失败: $fileName",
            )
            throw SevenZipException("failed to open file: $fileName")
        }
    }

    @Throws(SevenZipException::class)
    override fun write(data: ByteArray?): Int {
        if (data == null || data.isEmpty()) {
            return 0
        }

        val stream = ensureOutputStream()
        try {
            stream.write(data)
        } catch (e: IOException) {
            ErrorReportHelper.postCatchedException(
                e,
                "SequentialOutStream.write",
                "写入7z文件失败: $fileName",
            )
            close()
            outFile?.delete()
            throw SevenZipException("failed to write file: $fileName")
        }
        return data.size
    }

    override fun close() {
        val stream = outputStream ?: return
        outputStream = null
        try {
            stream.flush()
        } catch (ignore: IOException) {
        }
        try {
            stream.close()
        } catch (ignore: IOException) {
        }
    }
}

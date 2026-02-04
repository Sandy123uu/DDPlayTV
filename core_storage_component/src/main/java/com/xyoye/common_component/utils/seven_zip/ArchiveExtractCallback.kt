package com.xyoye.common_component.utils.seven_zip

import com.xyoye.common_component.utils.getFileName
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZipException
import java.io.Closeable
import java.io.File

class ArchiveExtractCallback(
    private val inArchive: IInArchive,
    private val destDir: File,
    private val callback: (destDirPath: String?) -> Unit
) : IArchiveExtractCallback,
    Closeable {
    private var totalProgress: Long = 0
    private var isCompleted = false
    private var currentOutStream: SequentialOutStream? = null

    @Throws(SevenZipException::class)
    override fun getStream(
        index: Int,
        extractAskMode: ExtractAskMode?
    ): ISequentialOutStream {
        val fileName: String =
            getFileName(inArchive.getProperty(index, PropID.PATH) as String)
        val outStream = SequentialOutStream(destDir, fileName)
        currentOutStream = outStream
        return outStream
    }

    override fun prepareOperation(extractAskMode: ExtractAskMode?) {}

    override fun setOperationResult(extractOperationResult: ExtractOperationResult) {
        currentOutStream?.close()
        currentOutStream = null
        if (!isCompleted && extractOperationResult !== ExtractOperationResult.OK) {
            isCompleted = true
            callback.invoke(null)
        }
    }

    override fun setTotal(total: Long) {
        totalProgress = total
    }

    override fun setCompleted(complete: Long) {
        if (complete == totalProgress) {
            if (!isCompleted) {
                isCompleted = true
                currentOutStream?.close()
                currentOutStream = null
                callback.invoke(destDir.absolutePath)
            }
        }
    }

    override fun close() {
        currentOutStream?.close()
        currentOutStream = null
    }
}

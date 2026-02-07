package com.xyoye.common_component.storage.file.impl

import android.net.Uri
import com.xyoye.common_component.extension.toMd5String
import com.xyoye.common_component.storage.file.AbstractStorageFile
import com.xyoye.common_component.storage.file.StorageFile
import com.xyoye.common_component.storage.impl.TorrentStorage
import com.xyoye.common_component.utils.getFileNameNoExtension
import com.xyoye.common_component.utils.thunder.model.ThunderTorrentFileInfo

/**
 * Created by xyoye on 2023/4/3
 */

internal class TorrentStorageFile(
    storage: TorrentStorage,
    private val fileInfo: ThunderTorrentFileInfo
) : AbstractStorageFile(storage) {
    override fun getRealFile(): Any = fileInfo

    fun getFileInfo(): ThunderTorrentFileInfo = fileInfo

    override fun filePath(): String = fileInfo.subPath

    override fun fileUrl(): String = Uri.parse(fileInfo.subPath).toString()

    override fun isDirectory(): Boolean = fileInfo.fileIndex == -1

    override fun fileName(): String = fileInfo.fileName

    override fun fileLength(): Long = fileInfo.fileSize

    override fun clone(): StorageFile =
        TorrentStorageFile(storage as TorrentStorage, fileInfo).also {
            it.playHistory = playHistory
        }

    override fun uniqueKey(): String {
        val hash = getFileNameNoExtension(fileInfo.subPath)
        return (hash + "_" + fileInfo.fileIndex).toMd5String()
    }
}

package com.xyoye.common_component.storage.file.impl

import com.xyoye.common_component.storage.file.AbstractStorageFile
import com.xyoye.common_component.storage.file.StorageFile
import com.xyoye.common_component.storage.impl.Cloud115Storage
import com.xyoye.data_component.data.cloud115.Cloud115FileInfo
import com.xyoye.common_component.utils.isVideoFile as isVideoFileByName

class Cloud115StorageFile(
    private val fileInfo: Cloud115FileInfo,
    private val parentPath: String,
    storage: Cloud115Storage,
    private val root: Boolean = false
) : AbstractStorageFile(storage) {
    override fun getRealFile(): Any = fileInfo

    override fun filePath(): String {
        if (root) return "/"

        val id = resolveId()
        val base = parentPath.removeSuffix("/")
        val path =
            if (base.isBlank()) {
                "/$id"
            } else {
                "$base/$id"
            }
        return if (isDirectory()) "$path/" else path
    }

    override fun fileUrl(): String = "115cloud://file/${resolveId()}"

    override fun isDirectory(): Boolean = root || fileInfo.fid?.trim().isNullOrBlank()

    override fun fileName(): String =
        fileInfo.n
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: if (root) {
                "根目录"
            } else {
                resolveId()
            }

    override fun fileLength(): Long = fileInfo.s ?: 0L

    override fun isRootFile(): Boolean = root

    override fun isVideoFile(): Boolean {
        if (isDirectory()) return false
        return isVideoFileByName(fileName())
    }

    override fun clone(): StorageFile =
        Cloud115StorageFile(
            fileInfo = fileInfo,
            parentPath = parentPath,
            storage = storage as Cloud115Storage,
            root = root,
        ).also {
            it.playHistory = playHistory
        }

    private fun resolveId(): String {
        if (root) return Cloud115Storage.ROOT_CID

        val fid = fileInfo.fid?.trim().orEmpty()
        if (fid.isNotBlank()) return fid

        return fileInfo.cid?.trim().orEmpty()
    }

    companion object {
        fun root(storage: Cloud115Storage): Cloud115StorageFile =
            Cloud115StorageFile(
                fileInfo =
                    Cloud115FileInfo(
                        cid = Cloud115Storage.ROOT_CID,
                        fid = null,
                        pid = null,
                        n = "根目录",
                        s = 0L,
                        pc = null,
                        t = null,
                    ),
                parentPath = "/",
                storage = storage,
                root = true,
            )
    }
}

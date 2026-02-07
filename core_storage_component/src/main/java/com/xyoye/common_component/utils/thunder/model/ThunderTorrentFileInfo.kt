package com.xyoye.common_component.utils.thunder.model

data class ThunderTorrentFileInfo(
    val fileIndex: Int,
    val fileName: String,
    val fileSize: Long,
    val subPath: String
) {
    companion object {
        fun createRoot(path: String): ThunderTorrentFileInfo =
            ThunderTorrentFileInfo(
                fileIndex = -1,
                fileName = path,
                fileSize = 0L,
                subPath = path
            )
    }
}

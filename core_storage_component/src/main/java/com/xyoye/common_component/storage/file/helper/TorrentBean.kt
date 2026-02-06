package com.xyoye.common_component.storage.file.helper

import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.utils.thunder.model.ThunderTorrentFileInfo
import com.xyoye.common_component.utils.thunder.model.ThunderTorrentInfo

/**
 * Created by xyoye on 2023/4/6
 */

internal data class TorrentBean(
    val torrentPath: String,
    val fileCount: Int,
    val infoHash: String,
    val isMultiFiles: Boolean,
    val multiFileBaseFolder: String,
    val subFileInfo: List<ThunderTorrentFileInfo>
) {

    companion object {
        fun formInfo(
            torrentPath: String,
            info: ThunderTorrentInfo
        ): TorrentBean =
            try {
                TorrentBean(
                    torrentPath = torrentPath,
                    fileCount = info.fileCount,
                    infoHash = info.infoHash,
                    isMultiFiles = info.isMultiFiles,
                    multiFileBaseFolder = info.multiFileBaseFolder,
                    subFileInfo =
                        info.subFileInfo.map {
                            it.copy(subPath = torrentPath)
                        },
                )
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "TorrentBean",
                    "formInfo",
                    "种子路径: $torrentPath",
                )
                throw e
            }
    }
}

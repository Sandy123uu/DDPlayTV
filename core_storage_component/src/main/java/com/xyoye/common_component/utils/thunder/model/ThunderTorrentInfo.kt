package com.xyoye.common_component.utils.thunder.model

data class ThunderTorrentInfo(
    val fileCount: Int,
    val infoHash: String,
    val isMultiFiles: Boolean,
    val multiFileBaseFolder: String,
    val subFileInfo: List<ThunderTorrentFileInfo>
)

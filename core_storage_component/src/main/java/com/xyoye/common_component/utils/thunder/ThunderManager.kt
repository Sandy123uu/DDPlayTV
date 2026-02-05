package com.xyoye.common_component.utils.thunder

import android.content.Context
import android.os.Build
import com.xunlei.downloadlib.XLDownloadManager
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.parameter.*
import com.xyoye.common_component.base.app.BaseApplication
import com.xyoye.common_component.extension.toMd5String
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.privacy.SensitiveDataSanitizer
import com.xyoye.common_component.storage.file.helper.TorrentBean
import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.utils.MagnetUtils
import com.xyoye.common_component.utils.PathHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by xyoye on 2021/11/20.
 */

class ThunderManager private constructor() {
    // 下载任务序号
    private val mAtomicInteger = AtomicInteger(0)

    // 下载任务列表
    private val mTaskList = hashMapOf<String, Long>()

    // 种子文件存储文件夹
    private val torrentDirectory = PathHelper.getTorrentDirectory()

    // 缓存文件存储文件夹
    private val cacheDirectory = PathHelper.getPlayCacheDirectory()

    companion object {
        private const val LOG_TAG = "ThunderManager"

        // thunder.aar 内置的 jni ABI（见 repository/thunder/thunder.aar）
        private val AAR_SUPPORTED_ABI = arrayOf("arm64-v8a", "armeabi-v7a")

        private val initLock = Any()

        @Volatile
        private var initAttempted: Boolean = false

        @Volatile
        private var initSucceeded: Boolean = false

        @Volatile
        private var initFailure: Throwable? = null

        // 无效的任务ID
        private const val INVALID_ID = -1L

        // 种子文件下载超时时间
        private const val TIME_OUT_DOWNLOAD_TORRENT = 5 * 1000L

        fun getInstance() = Holder.instance

        /**
         * 判断当前设备是否可能支持迅雷 SDK（仅做 ABI 预检，不会触发 SDK 初始化）。
         */
        fun isPlatformSupported(): Boolean {
            val supported = AAR_SUPPORTED_ABI.toHashSet()
            return Build.SUPPORTED_ABIS.any { supported.contains(it) }
        }

        /**
         * 按需初始化迅雷 SDK，幂等且可降级：
         * - 不支持 ABI：返回 false（不会抛异常）
         * - 初始化失败：返回 false（缓存失败原因，后续不再重复初始化）
         */
        fun ensureInitialized(): Boolean {
            if (initSucceeded) {
                return true
            }
            synchronized(initLock) {
                if (initSucceeded) {
                    return true
                }
                if (initAttempted) {
                    return false
                }
                initAttempted = true

                if (!isPlatformSupported()) {
                    initFailure =
                        IllegalStateException(
                            "Unsupported ABI: device=${Build.SUPPORTED_ABIS.joinToString()} supported=${AAR_SUPPORTED_ABI.joinToString()}",
                        )
                    LogFacade.w(
                        LogModule.STORAGE,
                        LOG_TAG,
                        "thunder init skipped: unsupported ABI",
                        mapOf(
                            "deviceAbis" to Build.SUPPORTED_ABIS.joinToString(),
                            "supportedAbis" to AAR_SUPPORTED_ABI.joinToString(),
                        ),
                    )
                    return false
                }

                return try {
                    XLTaskHelper.init(BaseApplication.getAppContext().applicationContext)
                    initSucceeded = true
                    LogFacade.i(LogModule.STORAGE, LOG_TAG, "thunder init success")
                    true
                } catch (t: Throwable) {
                    initFailure = t
                    LogFacade.e(
                        LogModule.STORAGE,
                        LOG_TAG,
                        "thunder init failed, feature will be disabled",
                        throwable = t,
                    )
                    false
                }
            }
        }

        fun initialize(context: Context) {
            // 兼容旧调用：改为幂等的按需初始化
            if (initSucceeded) return
            synchronized(initLock) {
                if (initSucceeded) return
                if (initAttempted) return
                initAttempted = true
                try {
                    XLTaskHelper.init(context.applicationContext)
                    initSucceeded = true
                } catch (t: Throwable) {
                    initFailure = t
                    LogFacade.e(
                        LogModule.STORAGE,
                        LOG_TAG,
                        "thunder init failed via initialize(context), feature will be disabled",
                        throwable = t,
                    )
                }
            }
        }

        /**
         * 用于 UI/调用方展示的可读降级原因（会触发按需初始化）。
         * @return `null` 表示已就绪，否则返回可展示的错误提示文案
         */
        fun ensureInitializedOrErrorMessage(): String? {
            if (ensureInitialized()) {
                return null
            }
            if (!isPlatformSupported()) {
                return "当前设备不支持迅雷磁链/BT（ABI 不匹配），已禁用该功能"
            }
            val t = initFailure
            return if (t == null) {
                "迅雷磁链/BT 初始化失败，已禁用该功能"
            } else {
                "迅雷磁链/BT 初始化失败（${t.javaClass.simpleName}），已禁用该功能"
            }
        }

        fun media3DownloadId(
            source: String,
            fileIndex: Int = -1
        ): String {
            val normalized =
                if (source.startsWith("magnet")) {
                    MagnetUtils.getMagnetHash(source).ifEmpty { source }
                } else {
                    source
                }
            val combined = if (fileIndex >= 0) "$normalized#$fileIndex" else normalized
            return combined.toMd5String()
        }
    }

    object Holder {
        val instance = ThunderManager()
    }

    /**
     * 下载种子文件
     * @param magnet 磁链
     * @return 种子文件路径
     */
    suspend fun downloadTorrentFile(magnet: String): String? {
        if (!ensureInitialized()) {
            return null
        }
        return try {
            val hash = MagnetUtils.getMagnetHash(magnet)
            if (hash.isEmpty()) {
                ErrorReportHelper.postException(
                    "Invalid magnet link format",
                    "ThunderManager",
                    RuntimeException("Magnet link hash extraction failed: ${SensitiveDataSanitizer.sanitizeMagnet(magnet)}"),
                )
                return null
            }

            val torrentTaskParam =
                MagnetTaskParam().apply {
                    setFileName("$hash.torrent")
                    setFilePath(torrentDirectory.absolutePath)
                    setUrl("magnet:?xt=urn:btih:$hash")
                }

            val torrentTaskId = XLTaskHelper.getInstance().addMagnetTask(torrentTaskParam)
            if (torrentTaskId == INVALID_ID) {
                ErrorReportHelper.postException(
                    "Failed to create torrent download task",
                    "ThunderManager",
                    RuntimeException(
                        "XLTaskHelper.addMagnetTask returned INVALID_ID for magnet: ${SensitiveDataSanitizer.sanitizeMagnet(magnet)}",
                    ),
                )
                return null
            }

            waitTorrentDownloaded(torrentTaskId, torrentTaskParam)
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "ThunderManager",
                "downloadTorrentFile",
                "磁链: ${SensitiveDataSanitizer.sanitizeMagnet(magnet)}",
            )
            null
        }
    }

    /**
     * 生成视频播放地址
     */
    suspend fun generatePlayUrl(
        torrent: TorrentBean,
        index: Int
    ): String? {
        if (!ensureInitialized()) {
            return null
        }
        return try {
            // 停止其它任务
            stopAllTask()

            // 启动下载任务
            val taskId = createPlayTask(torrent, index)
            if (taskId == INVALID_ID) {
                ErrorReportHelper.postException(
                    "Failed to create play task",
                    "ThunderManager",
                    RuntimeException(
                        "createPlayTask returned INVALID_ID for torrent: ${SensitiveDataSanitizer.sanitizePath(torrent.torrentPath)}, index: $index",
                    ),
                )
                return null
            }

            // 保存任务ID
            mTaskList[torrent.torrentPath] = taskId

            val fileName = torrent.mSubFileInfo.find { it.mFileIndex == index }?.mFileName ?: "temp.mp4"
            val filePath = "${cacheDirectory.absolutePath}/$fileName"
            val playUrl = XLTaskLocalUrl()
            XLDownloadManager.getInstance().getLocalUrl(filePath, playUrl)
            playUrl.mStrUrl
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "ThunderManager",
                "generatePlayUrl",
                "种子路径: ${SensitiveDataSanitizer.sanitizePath(torrent.torrentPath)}, 文件索引: $index",
            )
            null
        }
    }

    /**
     * 停止并移除任务
     */
    fun stopTask(taskId: Long) {
        if (!ensureInitialized()) {
            return
        }
        try {
            mTaskList.entries.find { it.value == taskId }?.let {
                mTaskList.remove(it.key)
            }
            XLTaskHelper.getInstance().deleteTask(taskId, cacheDirectory.absolutePath)
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "ThunderManager",
                "stopTask",
                "任务ID: $taskId",
            )
        }
    }

    /**
     * 停止所有任务
     */
    private fun stopAllTask() {
        if (!ensureInitialized()) {
            return
        }
        try {
            val iterator = mTaskList.iterator()
            while (iterator.hasNext()) {
                val entity = iterator.next()
                try {
                    XLTaskHelper.getInstance().deleteTask(entity.value, cacheDirectory.absolutePath)
                } catch (e: Exception) {
                    ErrorReportHelper.postCatchedExceptionWithContext(
                        e,
                        "ThunderManager",
                        "stopAllTask",
                        "删除单个任务失败，任务ID: ${entity.value}",
                    )
                }
                iterator.remove()
            }
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "ThunderManager",
                "stopAllTask",
                "停止所有任务时发生异常",
            )
        }
    }

    /**
     * 等待种子下载完成，超时5秒
     */
    private suspend fun waitTorrentDownloaded(
        torrentTaskId: Long,
        param: MagnetTaskParam
    ): String? {
        return try {
            withTimeoutOrNull(TIME_OUT_DOWNLOAD_TORRENT) {
                var taskInfo = XLTaskHelper.getInstance().getTaskInfo(torrentTaskId)
                while (taskInfo.mTaskStatus == XLConstant.XLTaskStatus.TASK_IDLE ||
                    taskInfo.mTaskStatus == XLConstant.XLTaskStatus.TASK_RUNNING
                ) {
                    delay(300L)
                    taskInfo = XLTaskHelper.getInstance().getTaskInfo(torrentTaskId)
                }
                try {
                    XLTaskHelper.getInstance().stopTask(torrentTaskId)
                } catch (e: Exception) {
                    ErrorReportHelper.postCatchedExceptionWithContext(
                        e,
                        "ThunderManager",
                        "waitTorrentDownloaded",
                        "停止种子下载任务失败，任务ID: $torrentTaskId",
                    )
                }
                return@withTimeoutOrNull if (
                    taskInfo.mTaskStatus == XLConstant.XLTaskStatus.TASK_SUCCESS
                ) {
                    "${param.mFilePath}/${param.mFileName}"
                } else {
                    ErrorReportHelper.postException(
                        "Torrent download failed",
                        "ThunderManager",
                        RuntimeException("Task status: ${taskInfo.mTaskStatus}, error code: ${taskInfo.mErrorCode}"),
                    )
                    null
                }
            } ?: run {
                ErrorReportHelper.postException(
                    "Torrent download timeout",
                    "ThunderManager",
                    RuntimeException("Timeout waiting for torrent download, task ID: $torrentTaskId"),
                )
                null
            }
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "ThunderManager",
                "waitTorrentDownloaded",
                "任务ID: $torrentTaskId, 文件名: ${param.mFileName}",
            )
            null
        }
    }

    /**
     * 创建下载任务
     */
    private suspend fun createPlayTask(
        torrent: TorrentBean,
        index: Int
    ): Long {
        val btTaskParam =
            BtTaskParam().apply {
                setCreateMode(1)
                setFilePath(cacheDirectory.absolutePath)
                setMaxConcurrent(1)
                setSeqId(mAtomicInteger.incrementAndGet())
                setTorrentPath(torrent.torrentPath)
            }

        val deselectedIndexes =
            torrent.mSubFileInfo
                .filter { it.mFileIndex != index }
                .map { it.mFileIndex }
                .toIntArray()

        val selectedIndexSet =
            BtIndexSet(1).apply {
                mIndexSet = IntArray(index)
            }
        val deselectIndexSet =
            BtIndexSet(deselectedIndexes.size).apply {
                mIndexSet = deselectedIndexes
            }

        return createTorrentTask(btTaskParam, selectedIndexSet, deselectIndexSet)
    }

    /**
     * 启动下载任务
     */
    private suspend fun createTorrentTask(
        btTaskParam: BtTaskParam,
        selectedIndexes: BtIndexSet,
        deSelectIndexes: BtIndexSet
    ): Long {
        return try {
            // 启动下载任务
            var taskId =
                XLTaskHelper
                    .getInstance()
                    .startTask(btTaskParam, selectedIndexes, deSelectIndexes)
            if (taskId == INVALID_ID) {
                stopTask(taskId)
                delay(200)
                taskId =
                    XLTaskHelper
                        .getInstance()
                        .startTask(btTaskParam, selectedIndexes, deSelectIndexes)
            }

            if (taskId == INVALID_ID) {
                stopTask(taskId)
                ErrorReportHelper.postException(
                    "Failed to start torrent task after retry",
                    "ThunderManager",
                    RuntimeException("XLTaskHelper.startTask returned INVALID_ID twice for torrent: ${btTaskParam.mTorrentPath}"),
                )
                return INVALID_ID
            }

            // 任务无法下载
            if (checkTaskFailed(taskId)) {
                stopTask(taskId)
                ErrorReportHelper.postException(
                    "Torrent task failed during check",
                    "ThunderManager",
                    RuntimeException("Task failed check, task ID: $taskId, torrent: ${btTaskParam.mTorrentPath}"),
                )
                return INVALID_ID
            }

            taskId
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "ThunderManager",
                "createTorrentTask",
                "种子路径: ${btTaskParam.mTorrentPath}",
            )
            INVALID_ID
        }
    }

    /**
     * 检查下载任务是否失败
     */
    private suspend fun checkTaskFailed(taskId: Long): Boolean {
        return try {
            delay(2000)

            // 任务下载失败
            val taskInfo = XLTaskHelper.getInstance().getTaskInfo(taskId)
            val taskStatus = taskInfo.mTaskStatus
            if (taskStatus == XLConstant.XLTaskStatus.TASK_FAILED) {
                ErrorReportHelper.postException(
                    "Download task failed",
                    "ThunderManager",
                    RuntimeException("Task ID: $taskId, Status: $taskStatus, Error Code: ${taskInfo.mErrorCode}"),
                )
                return true
            }

            false
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "ThunderManager",
                "checkTaskFailed",
                "任务ID: $taskId",
            )
            true // 如果检查过程出错，认为任务失败
        }
    }

    /**
     * 获取种子文件信息
     */
    fun getTaskInfo(torrentPath: String): TorrentInfo =
        try {
            if (!ensureInitialized()) {
                throw IllegalStateException(ensureInitializedOrErrorMessage() ?: "Thunder is not initialized")
            }
            XLTaskHelper.getInstance().getTorrentInfo(torrentPath)
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "ThunderManager",
                "getTaskInfo",
                "种子路径: $torrentPath",
            )
            // 返回默认的空TorrentInfo或重新抛出异常
            throw e
        }

    /**
     * 获取下载任务信息
     */
    fun getTaskInfo(taskId: Long): XLTaskInfo =
        try {
            if (!ensureInitialized()) {
                throw IllegalStateException(ensureInitializedOrErrorMessage() ?: "Thunder is not initialized")
            }
            XLTaskHelper.getInstance().getTaskInfo(taskId)
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "ThunderManager",
                "getTaskInfo",
                "任务ID: $taskId",
            )
            // 返回默认的空XLTaskInfo或重新抛出异常
            throw e
        }

    /**
     * 获取下载任务ID
     */
    fun getTaskId(torrentPath: String): Long = mTaskList[torrentPath] ?: INVALID_ID
}

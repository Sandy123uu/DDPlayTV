package com.xyoye.common_component.network.repository

import android.util.Base64
import com.xyoye.common_component.network.RetrofitManager
import com.xyoye.common_component.network.request.NetworkException
import com.xyoye.common_component.storage.helper.ScreencastConstants
import com.xyoye.common_component.utils.EntropyUtils
import com.xyoye.common_component.utils.JsonHelper
import com.xyoye.data_component.data.CommonJsonData
import com.xyoye.data_component.data.screeencast.ScreencastData

/**
 * Created by xyoye on 2024/1/11.
 */

object ScreencastRepository : BaseRepository() {
    /**
     * 投屏初始化
     */
    suspend fun init(
        url: String,
        password: String?
    ): Result<retrofit2.Response<CommonJsonData>> {
        val normalizedPassword = password?.takeIf { it.isNotBlank() }
        val v2Authorization = normalizedPassword?.let { buildV2Authorization(it) }
        val v2Result = initWithAuthorization(url, v2Authorization)
        if (v2Result.isSuccess) {
            return v2Result
        }

        val exception = v2Result.exceptionOrNull() as? NetworkException
        if (exception?.code != 401 || normalizedPassword == null) {
            return v2Result
        }

        val legacyAuthorization = buildLegacyAuthorization(normalizedPassword) ?: return v2Result
        return initWithAuthorization(url, legacyAuthorization)
    }

    private suspend fun initWithAuthorization(
        url: String,
        authorization: String?
    ) = request()
        .doGet {
            RetrofitManager.screencastService.init(url, authorization, ScreencastConstants.version)
        }.run {
            // 数据错误，外部处理
            val response = getOrNull() ?: return@run this
            val data = response.body() ?: return@run this

            // 服务器返回错误，转换为Response.Error
            if (data.success.not()) {
                return@run Result.failure(NetworkException.formJsonData(data))
            }

            // 版本版本判断
            val remoteVersion = response.headers()[ScreencastConstants.Header.versionKey]?.toIntOrNull() ?: 0
            // 版本相同，返回Response.Success
            val localVersion = ScreencastConstants.version
            if (localVersion == remoteVersion) {
                return@run this
            }

            // 版本不同，返回Response.Error
            val message =
                "投屏版本不匹配，请更新双端至相同APP版本。" +
                    "\n投屏端: $localVersion，接收端: $remoteVersion"
            return@run Result.failure(NetworkException.formException(IllegalStateException(message)))
        }

    private fun buildV2Authorization(password: String): String? {
        val token =
            EntropyUtils.aesEncode(
                key = password,
                content = password,
                base64Flag = Base64.NO_WRAP,
                version = EntropyUtils.AES_VERSION_GCM_V2,
            ) ?: return null
        return "Bearer $token"
    }

    private fun buildLegacyAuthorization(password: String): String? {
        val token =
            EntropyUtils.aesEncode(
                key = null,
                content = password,
                base64Flag = Base64.NO_WRAP,
                version = EntropyUtils.AES_VERSION_LEGACY_CBC_V1,
                allowLegacyDefaultKeyFallback = true,
            ) ?: return null
        return "Bearer $token"
    }

    /**
     * 投屏播放
     */
    suspend fun play(
        url: String,
        authorization: String?,
        data: ScreencastData
    ) = request()
        .json(JsonHelper.toJson(data).orEmpty())
        .doPost {
            RetrofitManager.screencastService.play(url, authorization, it)
        }
}

package com.xyoye.common_component.storage.cloud115.auth

import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.config.Api
import com.xyoye.common_component.storage.cloud115.net.Cloud115Headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Cloud115TokenValidator {
    private const val LOG_TAG = "cloud115_token_validator"

    suspend fun validate(cookieHeader: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cookie = cookieHeader.trim()
                if (cookie.isBlank()) {
                    throw IllegalArgumentException("token 为空")
                }

                val resp =
                    Retrofit.cloud115Service.cookieStatus(
                        baseUrl = Api.CLOUD_115_MY,
                        cookie = cookie,
                        ct = "guide",
                        ac = "status",
                        timestamp = System.currentTimeMillis().toString(),
                    )

                if (!resp.state) {
                    throw IllegalStateException("token 无效或已过期")
                }
            }.onFailure { t ->
                LogFacade.w(
                    LogModule.STORAGE,
                    LOG_TAG,
                    "validate token failed",
                    mapOf(
                        "cookie" to Cloud115Headers.redactCookie(cookieHeader),
                        "exception" to t::class.java.simpleName,
                    ),
                    t,
                )
            }
        }
}

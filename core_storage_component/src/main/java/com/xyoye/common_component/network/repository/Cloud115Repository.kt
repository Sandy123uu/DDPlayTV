package com.xyoye.common_component.network.repository

import com.squareup.moshi.Json
import com.squareup.moshi.JsonEncodingException
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.network.RetrofitManager
import com.xyoye.common_component.network.config.Api
import com.xyoye.common_component.storage.cloud115.auth.Cloud115AuthStore
import com.xyoye.common_component.storage.cloud115.auth.Cloud115NotConfiguredException
import com.xyoye.common_component.storage.cloud115.auth.Cloud115ReAuthRequiredException
import com.xyoye.common_component.storage.cloud115.crypto.M115Crypto
import com.xyoye.common_component.storage.cloud115.net.Cloud115Headers
import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.utils.JsonHelper
import com.xyoye.data_component.data.cloud115.Cloud115CookieStatusResp
import com.xyoye.data_component.data.cloud115.Cloud115DownloadResp
import com.xyoye.data_component.data.cloud115.Cloud115FileListResp
import com.xyoye.data_component.data.cloud115.Cloud115FileStatResponse
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeLoginResp
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeStatusResp
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeTokenResp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.nio.charset.StandardCharsets

class Cloud115Repository(
    private val storageKey: String
) : BaseRepository() {
    data class PlayUrl(
        val url: String,
        val userAgent: String
    )

    private var lastCookieCheckAtMs: Long = 0L
    private var lastCookieValid: Boolean = false

    fun isAuthorized(): Boolean = Cloud115AuthStore.read(storageKey).isAuthorized()

    suspend fun qrcodeToken(): Result<Cloud115QRCodeTokenResp> =
        requestQrCodeApi(reason = "qrcodeToken") {
            val response =
                RetrofitManager.cloud115Service.qrcodeToken(
                    baseUrl = Api.CLOUD_115_QRCODE_API,
                )

            if (!isQrCodeSuccess(response.state, response.code)) {
                throw IllegalStateException("获取二维码失败（code=${response.code} state=${response.state}）")
            }

            response
        }

    suspend fun qrcodeImage(uid: String): Result<ResponseBody> =
        requestQrCodeApi(
            reason = "qrcodeImage",
            extraInfo = "uidLength=${uid.length}",
        ) {
            RetrofitManager.cloud115Service.qrcodeImage(
                baseUrl = Api.CLOUD_115_QRCODE_API,
                uid = uid,
            )
        }

    suspend fun qrcodeStatus(
        uid: String,
        time: Long,
        sign: String
    ): Result<Cloud115QRCodeStatusResp> =
        requestQrCodeApi(
            reason = "qrcodeStatus",
            extraInfo = "uidLength=${uid.length} time=$time signLength=${sign.length}",
        ) {
            val response =
                RetrofitManager.cloud115Service.qrcodeStatus(
                    baseUrl = Api.CLOUD_115_QRCODE_API,
                    uid = uid,
                    time = time,
                    sign = sign,
                    timestamp = System.currentTimeMillis().toString(),
                )

            if (!isQrCodeSuccess(response.state, response.code)) {
                throw IllegalStateException("获取二维码状态失败（code=${response.code} state=${response.state}）")
            }

            response
        }

    suspend fun qrcodeLogin(
        uid: String,
        app: String = DEFAULT_QRCODE_APP,
        persistAuth: Boolean = true
    ): Result<Cloud115QRCodeLoginResp> =
        requestPassportApi(
            reason = "qrcodeLogin",
            extraInfo = "uidLength=${uid.length} app=$app",
        ) {
            val response =
                RetrofitManager.cloud115Service.qrcodeLogin(
                    baseUrl = Api.CLOUD_115_PASSPORT_API,
                    app = app,
                    account = uid,
                    appInForm = app,
                )

            if (!isQrCodeSuccess(response.state, response.code)) {
                throw IllegalStateException("扫码登录失败（code=${response.code} state=${response.state}）")
            }

            val cookieHeader = Cloud115Headers.buildCookieHeader(response.data?.cookie)
            val userId =
                response.data
                    ?.userId
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            if (cookieHeader.isBlank() || userId.isBlank()) {
                throw IllegalStateException("扫码登录返回数据异常")
            }

            val avatarUrl =
                response.data?.face?.faceLarge
                    ?: response.data?.face?.faceMedium
                    ?: response.data?.face?.faceSmall

            if (persistAuth) {
                Cloud115AuthStore.writeAuthorized(
                    storageKey = storageKey,
                    cookie = cookieHeader,
                    userId = userId,
                    loginApp = app,
                    userName = response.data?.userName,
                    avatarUrl = avatarUrl,
                    updatedAtMs = System.currentTimeMillis(),
                )

                lastCookieCheckAtMs = System.currentTimeMillis()
                lastCookieValid = true
            }
            LogFacade.i(
                LogModule.STORAGE,
                LOG_TAG,
                "qrcode login success",
                mapOf(
                    "storageKey" to storageKey,
                    "userId" to userId,
                    "loginApp" to app,
                    "cookie" to Cloud115Headers.redactCookie(cookieHeader),
                ),
            )

            response
        }

    suspend fun cookieStatus(forceCheck: Boolean = false): Result<Cloud115CookieStatusResp> =
        requestMyApi(reason = "cookieStatus", extraInfo = "forceCheck=$forceCheck") {
            ensureCookieValid(forceCheck = forceCheck)
        }

    suspend fun listFiles(
        cid: String,
        limit: Int,
        offset: Int,
        order: String? = null,
        asc: Int? = null,
        showDir: Int = 1
    ): Result<Cloud115FileListResp> =
        requestWebApi(
            reason = "listFiles",
            extraInfo = "cid=$cid limit=$limit offset=$offset order=$order asc=${asc ?: -1} showDir=$showDir",
        ) { cookie ->
            val response =
                RetrofitManager.cloud115Service.listFiles(
                    baseUrl = Api.CLOUD_115_WEB_API,
                    cookie = cookie,
                    userAgent = Cloud115Headers.USER_AGENT,
                    aid = "1",
                    cid = cid,
                    offset = offset,
                    limit = limit,
                    showDir = showDir,
                    order = order,
                    asc = asc,
                    format = "json",
                )

            if (!response.state) {
                ensureCookieValid(forceCheck = true)
                throw IllegalStateException(buildApiErrorMessage(response.error, response.msg, "获取目录列表失败"))
            }

            response
        }

    suspend fun searchFiles(
        searchValue: String,
        cid: String,
        type: Int? = null,
        countFolders: Int? = null,
        offset: Int? = null,
        limit: Int? = null,
        order: String? = null,
        asc: Int? = null
    ): Result<Cloud115FileListResp> =
        requestWebApi(
            reason = "searchFiles",
            extraInfo = "keywordLength=${searchValue.length} cid=$cid offset=${offset ?: -1} limit=${limit ?: -1} order=$order asc=${asc ?: -1}",
        ) { cookie ->
            val response =
                RetrofitManager.cloud115Service.searchFiles(
                    baseUrl = Api.CLOUD_115_WEB_API,
                    cookie = cookie,
                    userAgent = Cloud115Headers.USER_AGENT,
                    searchValue = searchValue,
                    cid = cid,
                    type = type,
                    countFolders = countFolders,
                    offset = offset,
                    limit = limit,
                    order = order,
                    asc = asc,
                    format = "json",
                )

            if (!response.state) {
                ensureCookieValid(forceCheck = true)
                throw IllegalStateException(buildApiErrorMessage(response.error, response.msg, "搜索失败"))
            }

            response
        }

    suspend fun stat(cid: String): Result<Cloud115FileStatResponse> =
        requestWebApi(
            reason = "stat",
            extraInfo = "cid=$cid",
        ) { cookie ->
            RetrofitManager.cloud115Service.stat(
                baseUrl = Api.CLOUD_115_WEB_API,
                cookie = cookie,
                userAgent = Cloud115Headers.USER_AGENT,
                cid = cid,
            )
        }

    suspend fun downloadUrl(
        pickCode: String,
        userAgent: String = Cloud115Headers.USER_AGENT
    ): Result<PlayUrl> =
        requestProApi(
            reason = "downloadUrl",
            extraInfo = "pickCodeLength=${pickCode.length} uaLength=${userAgent.length}",
        ) { cookie ->
            val key = M115Crypto.generateKey()
            val payload =
                JsonHelper.toJson(DownloadUrlPayload(pickCode = pickCode))
                    ?: throw IllegalStateException("构建请求参数失败")
            val encryptedData = M115Crypto.encode(payload.toByteArray(StandardCharsets.UTF_8), key)

            val response =
                RetrofitManager.cloud115Service.downloadUrl(
                    baseUrl = Api.CLOUD_115_PRO_API,
                    cookie = cookie,
                    userAgent = userAgent,
                    t = System.currentTimeMillis().toString(),
                    data = encryptedData,
                )

            if (!response.state) {
                ensureCookieValid(forceCheck = true)
                throw IllegalStateException(buildApiErrorMessage(response.error, response.msg, "获取播放链接失败"))
            }

            val decoded =
                decodeDownloadResponse(response, key)
                    ?: throw IllegalStateException("获取播放链接失败")

            PlayUrl(
                url = decoded.url,
                userAgent = userAgent,
            )
        }

    private suspend fun ensureCookieValid(forceCheck: Boolean): Cloud115CookieStatusResp {
        val nowMs = System.currentTimeMillis()
        if (!forceCheck && lastCookieCheckAtMs > 0L && nowMs - lastCookieCheckAtMs < COOKIE_CHECK_TTL_MS) {
            if (!lastCookieValid) {
                throw Cloud115ReAuthRequiredException("115 Cloud 授权已失效，请重新授权")
            }
            return Cloud115CookieStatusResp(state = true)
        }

        val cookie = requireCookie()
        val response =
            try {
                RetrofitManager.cloud115Service.cookieStatus(
                    baseUrl = Api.CLOUD_115_MY,
                    cookie = cookie,
                    userAgent = Cloud115Headers.USER_AGENT,
                    ct = "guide",
                    ac = "status",
                    timestamp = nowMs.toString(),
                )
            } catch (e: JsonEncodingException) {
                lastCookieCheckAtMs = nowMs
                lastCookieValid = false
                throw Cloud115ReAuthRequiredException("授权校验失败（返回非 JSON），请重试或重新授权")
            }

        lastCookieCheckAtMs = nowMs
        lastCookieValid = response.state

        if (!response.state) {
            throw Cloud115ReAuthRequiredException("115 Cloud 授权已失效，请重新授权")
        }

        return response
    }

    private fun requireCookie(): String {
        val state = Cloud115AuthStore.read(storageKey)
        if (!state.isAuthorized()) {
            throw Cloud115NotConfiguredException("请先完成授权")
        }
        val cookie = state.cookie?.trim().orEmpty()
        if (cookie.isBlank()) {
            throw Cloud115NotConfiguredException("请先完成授权")
        }
        return cookie
    }

    private fun redactedCookie(): String =
        runCatching { Cloud115Headers.redactCookie(Cloud115AuthStore.read(storageKey).cookie) }
            .getOrDefault("")

    private fun decodeDownloadResponse(
        response: Cloud115DownloadResp,
        key: ByteArray
    ): DecodedDownloadUrl? {
        val encrypted = response.data?.trim().orEmpty()
        if (encrypted.isBlank()) {
            return null
        }

        val decodedBytes = M115Crypto.decode(encrypted, key)
        val json = String(decodedBytes, StandardCharsets.UTF_8)
        return JsonHelper
            .parseJson<DecodedDownloadUrl>(json)
            ?.takeIf { it.url.isNotBlank() }
    }

    private suspend fun <T> requestQrCodeApi(
        reason: String,
        extraInfo: String = "",
        call: suspend () -> T
    ): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                call.invoke()
            }.onFailure { t ->
                LogFacade.e(
                    LogModule.STORAGE,
                    LOG_TAG,
                    "qrcode api request failed: $reason",
                    mapOf(
                        "storageKey" to storageKey,
                        "extraInfo" to extraInfo,
                        "exception" to t::class.java.simpleName,
                    ),
                    t,
                )
                ErrorReportHelper.postCatchedExceptionWithContext(
                    t,
                    "Cloud115Repository",
                    reason,
                    "storageKey=$storageKey $extraInfo",
                )
            }
        }

    private suspend fun <T> requestPassportApi(
        reason: String,
        extraInfo: String = "",
        call: suspend () -> T
    ): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                call.invoke()
            }.onFailure { t ->
                LogFacade.e(
                    LogModule.STORAGE,
                    LOG_TAG,
                    "passport api request failed: $reason",
                    mapOf(
                        "storageKey" to storageKey,
                        "extraInfo" to extraInfo,
                        "exception" to t::class.java.simpleName,
                    ),
                    t,
                )
                ErrorReportHelper.postCatchedExceptionWithContext(
                    t,
                    "Cloud115Repository",
                    reason,
                    "storageKey=$storageKey $extraInfo",
                )
            }
        }

    private suspend fun <T> requestMyApi(
        reason: String,
        extraInfo: String = "",
        call: suspend () -> T
    ): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                call.invoke()
            }.onFailure { t ->
                LogFacade.e(
                    LogModule.STORAGE,
                    LOG_TAG,
                    "my api request failed: $reason",
                    mapOf(
                        "storageKey" to storageKey,
                        "extraInfo" to extraInfo,
                        "cookie" to redactedCookie(),
                        "exception" to t::class.java.simpleName,
                    ),
                    t,
                )
                ErrorReportHelper.postCatchedExceptionWithContext(
                    t,
                    "Cloud115Repository",
                    reason,
                    "storageKey=$storageKey cookie=${redactedCookie()} $extraInfo",
                )
            }
        }

    private suspend fun <T> requestWebApi(
        reason: String,
        extraInfo: String = "",
        call: suspend (cookie: String) -> T
    ): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cookie = requireCookie()
                call.invoke(cookie)
            }.onFailure { t ->
                LogFacade.e(
                    LogModule.STORAGE,
                    LOG_TAG,
                    "webapi request failed: $reason",
                    mapOf(
                        "storageKey" to storageKey,
                        "extraInfo" to extraInfo,
                        "cookie" to redactedCookie(),
                        "exception" to t::class.java.simpleName,
                    ),
                    t,
                )
                ErrorReportHelper.postCatchedExceptionWithContext(
                    t,
                    "Cloud115Repository",
                    reason,
                    "storageKey=$storageKey cookie=${redactedCookie()} $extraInfo",
                )
            }
        }

    private suspend fun <T> requestProApi(
        reason: String,
        extraInfo: String = "",
        call: suspend (cookie: String) -> T
    ): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cookie = requireCookie()
                call.invoke(cookie)
            }.onFailure { t ->
                LogFacade.e(
                    LogModule.STORAGE,
                    LOG_TAG,
                    "proapi request failed: $reason",
                    mapOf(
                        "storageKey" to storageKey,
                        "extraInfo" to extraInfo,
                        "cookie" to redactedCookie(),
                        "exception" to t::class.java.simpleName,
                    ),
                    t,
                )
                ErrorReportHelper.postCatchedExceptionWithContext(
                    t,
                    "Cloud115Repository",
                    reason,
                    "storageKey=$storageKey cookie=${redactedCookie()} $extraInfo",
                )
            }
        }

    private fun isQrCodeSuccess(
        state: Int,
        code: Int
    ): Boolean {
        if (state == 1) {
            return true
        }
        return code == 0 && state != 0
    }

    private fun buildApiErrorMessage(
        error: String?,
        msg: String?,
        fallback: String
    ): String {
        val detail =
            error?.trim()?.takeIf { it.isNotBlank() }
                ?: msg?.trim()?.takeIf { it.isNotBlank() }
        return if (detail.isNullOrBlank()) {
            fallback
        } else {
            "$fallback：$detail"
        }
    }

    private data class DecodedDownloadUrl(
        val url: String = ""
    )

    private data class DownloadUrlPayload(
        @Json(name = "pick_code")
        val pickCode: String
    )

    companion object {
        private const val LOG_TAG = "cloud115_repo"
        private const val DEFAULT_QRCODE_APP: String = "tv"
        private const val COOKIE_CHECK_TTL_MS: Long = 30_000L
    }
}

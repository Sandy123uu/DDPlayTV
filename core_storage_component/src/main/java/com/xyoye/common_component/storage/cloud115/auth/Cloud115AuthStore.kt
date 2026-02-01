package com.xyoye.common_component.storage.cloud115.auth

import com.tencent.mmkv.MMKV
import com.xyoye.data_component.entity.MediaLibraryEntity

/**
 * 115 Cloud 授权态存储（按媒体库 storageKey 隔离）。
 *
 * 设计说明：
 * - 按媒体库唯一键隔离：storageKey = "${mediaType.value}:${url.trim().removeSuffix("/")}"
 * - 不把 Cookie 写入 MediaLibraryEntity 通用字段（避免语义污染与泄露风险）
 */
object Cloud115AuthStore {
    private const val MMKV_ID = "cloud115_auth_store"

    private const val KEY_COOKIE = "cookie"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_AVATAR_URL = "avatar_url"
    private const val KEY_UPDATED_AT_MS = "updated_at_ms"

    data class AuthState(
        val cookie: String? = null,
        val userId: String? = null,
        val userName: String? = null,
        val avatarUrl: String? = null,
        val updatedAtMs: Long = 0L
    ) {
        fun isAuthorized(): Boolean = cookie.isNullOrBlank().not() && userId.isNullOrBlank().not()
    }

    fun storageKey(library: MediaLibraryEntity): String = "${library.mediaType.value}:${library.url.trim().removeSuffix("/")}"

    fun read(storageKey: String): AuthState {
        val kv = mmkv()
        val cookie = kv.decodeString(namespacedKey(storageKey, KEY_COOKIE))
        val userId = kv.decodeString(namespacedKey(storageKey, KEY_USER_ID))
        val userName = kv.decodeString(namespacedKey(storageKey, KEY_USER_NAME))
        val avatarUrl = kv.decodeString(namespacedKey(storageKey, KEY_AVATAR_URL))
        val updatedAtMs = kv.decodeLong(namespacedKey(storageKey, KEY_UPDATED_AT_MS), 0L)

        return AuthState(
            cookie = cookie,
            userId = userId,
            userName = userName,
            avatarUrl = avatarUrl,
            updatedAtMs = updatedAtMs,
        )
    }

    fun writeAuthorized(
        storageKey: String,
        cookie: String?,
        userId: String?,
        userName: String?,
        avatarUrl: String?,
        updatedAtMs: Long = System.currentTimeMillis()
    ) {
        val kv = mmkv()
        cookie?.let { kv.encode(namespacedKey(storageKey, KEY_COOKIE), it) }
        userId?.let { kv.encode(namespacedKey(storageKey, KEY_USER_ID), it) }
        userName?.let { kv.encode(namespacedKey(storageKey, KEY_USER_NAME), it) }
        avatarUrl?.let { kv.encode(namespacedKey(storageKey, KEY_AVATAR_URL), it) }
        kv.encode(namespacedKey(storageKey, KEY_UPDATED_AT_MS), updatedAtMs)
    }

    fun clear(storageKey: String) {
        val kv = mmkv()
        kv.removeValueForKey(namespacedKey(storageKey, KEY_COOKIE))
        kv.removeValueForKey(namespacedKey(storageKey, KEY_USER_ID))
        kv.removeValueForKey(namespacedKey(storageKey, KEY_USER_NAME))
        kv.removeValueForKey(namespacedKey(storageKey, KEY_AVATAR_URL))
        kv.removeValueForKey(namespacedKey(storageKey, KEY_UPDATED_AT_MS))
    }

    private fun mmkv(): MMKV = MMKV.mmkvWithID(MMKV_ID)

    private fun namespacedKey(
        storageKey: String,
        fieldKey: String
    ): String = "$storageKey.$fieldKey"
}


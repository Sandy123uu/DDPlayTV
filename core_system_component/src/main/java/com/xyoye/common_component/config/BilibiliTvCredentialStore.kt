package com.xyoye.common_component.config

import com.xyoye.core_system_component.BuildConfig

object BilibiliTvCredentialStore {
    fun isBuildCredentialInjected(): Boolean = BuildConfig.BILIBILI_TV_CREDENTIAL_INJECTED

    fun getAppKey(): String? {
        if (isBuildCredentialInjected()) {
            return BuildConfig.BILIBILI_TV_APP_KEY.trim().takeIf { it.isNotEmpty() }
        }

        return getCredential(
            encrypted = DevelopConfig.getBilibiliTvAppKeyEncrypted(),
            legacyPlain = DevelopConfig.getBilibiliTvAppKey(),
            saveEncrypted = DevelopConfig::putBilibiliTvAppKeyEncrypted,
            clearLegacyPlain = { DevelopConfig.putBilibiliTvAppKey("") },
        )
    }

    fun getAppSecret(): String? {
        if (isBuildCredentialInjected()) {
            return BuildConfig.BILIBILI_TV_APP_SECRET.trim().takeIf { it.isNotEmpty() }
        }

        return getCredential(
            encrypted = DevelopConfig.getBilibiliTvAppSecretEncrypted(),
            legacyPlain = DevelopConfig.getBilibiliTvAppSecret(),
            saveEncrypted = DevelopConfig::putBilibiliTvAppSecretEncrypted,
            clearLegacyPlain = { DevelopConfig.putBilibiliTvAppSecret("") },
        )
    }

    /**
     * 仅用于输入框回显：只回显本地保存的值，不回显编译期注入的值，避免在UI里直接暴露。
     */
    fun getStoredAppKeyForPrefill(): String? =
        getStoredCredentialForPrefill(
            encrypted = DevelopConfig.getBilibiliTvAppKeyEncrypted(),
            legacyPlain = DevelopConfig.getBilibiliTvAppKey(),
        )

    /**
     * 仅用于输入框回显：只回显本地保存的值，不回显编译期注入的值，避免在UI里直接暴露。
     */
    fun getStoredAppSecretForPrefill(): String? =
        getStoredCredentialForPrefill(
            encrypted = DevelopConfig.getBilibiliTvAppSecretEncrypted(),
            legacyPlain = DevelopConfig.getBilibiliTvAppSecret(),
        )

    fun putAppKey(appKey: String) {
        putCredential(
            value = appKey,
            saveEncrypted = DevelopConfig::putBilibiliTvAppKeyEncrypted,
            clearLegacyPlain = { DevelopConfig.putBilibiliTvAppKey("") },
            saveLegacyPlain = DevelopConfig::putBilibiliTvAppKey,
        )
    }

    fun putAppSecret(appSecret: String) {
        putCredential(
            value = appSecret,
            saveEncrypted = DevelopConfig::putBilibiliTvAppSecretEncrypted,
            clearLegacyPlain = { DevelopConfig.putBilibiliTvAppSecret("") },
            saveLegacyPlain = DevelopConfig::putBilibiliTvAppSecret,
        )
    }

    private fun getCredential(
        encrypted: String?,
        legacyPlain: String?,
        saveEncrypted: (String) -> Unit,
        clearLegacyPlain: () -> Unit
    ): String? {
        if (!encrypted.isNullOrBlank()) {
            CredentialCrypto.decrypt(encrypted)?.let { return it }
        }

        if (!legacyPlain.isNullOrBlank()) {
            CredentialCrypto.encrypt(legacyPlain)?.let { encryptedValue ->
                saveEncrypted(encryptedValue)
                clearLegacyPlain()
            }
            return legacyPlain
        }

        return null
    }

    private fun getStoredCredentialForPrefill(
        encrypted: String?,
        legacyPlain: String?
    ): String? {
        if (!encrypted.isNullOrBlank()) {
            CredentialCrypto.decrypt(encrypted)?.let { return it }
        }
        return legacyPlain?.takeIf { it.isNotBlank() }
    }

    private fun putCredential(
        value: String,
        saveEncrypted: (String) -> Unit,
        clearLegacyPlain: () -> Unit,
        saveLegacyPlain: (String) -> Unit
    ) {
        if (value.isBlank()) {
            saveEncrypted("")
            clearLegacyPlain()
            return
        }

        val encrypted = CredentialCrypto.encrypt(value)
        if (encrypted != null) {
            saveEncrypted(encrypted)
            clearLegacyPlain()
            return
        }

        // 加密失败：兜底仍然保存明文，避免功能不可用
        saveLegacyPlain(value)
        saveEncrypted("")
    }
}

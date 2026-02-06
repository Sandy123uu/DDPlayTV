package com.xyoye.common_component.config

import com.xyoye.core_system_component.BuildConfig

object DeveloperCredentialStore {
    enum class SaveResult {
        SAVED_ENCRYPTED,
        SAVED_WITH_PLAINTEXT_FALLBACK,
        REJECTED_ENCRYPTION_FAILED
    }

    data class PlaintextMigrationResult(
        val migratedCount: Int,
        val failedCount: Int
    )

    fun isBuildCredentialInjected(): Boolean = BuildConfig.DANDAN_DEV_CREDENTIAL_INJECTED

    fun isPlaintextFallbackSwitchVisible(): Boolean = BuildConfig.DEBUG

    fun isPlaintextFallbackEnabled(): Boolean = BuildConfig.DEBUG && DevelopConfig.isCredentialPlaintextFallbackEnabled()

    fun setPlaintextFallbackEnabled(enabled: Boolean) {
        DevelopConfig.putCredentialPlaintextFallbackEnabled(enabled && BuildConfig.DEBUG)
    }

    fun hasLegacyPlaintextCredentials(): Boolean =
        !DevelopConfig.getAppId().isNullOrBlank() ||
            !DevelopConfig.getAppSecret().isNullOrBlank()

    fun migrateLegacyPlaintextCredentials(): PlaintextMigrationResult {
        var migratedCount = 0
        var failedCount = 0

        if (migrateSingleLegacyCredential(
                legacyPlain = DevelopConfig.getAppId(),
                saveEncrypted = DevelopConfig::putAppIdEncrypted,
                clearLegacyPlain = { DevelopConfig.putAppId("") },
            )
        ) {
            migratedCount++
        } else if (!DevelopConfig.getAppId().isNullOrBlank()) {
            failedCount++
        }

        if (migrateSingleLegacyCredential(
                legacyPlain = DevelopConfig.getAppSecret(),
                saveEncrypted = DevelopConfig::putAppSecretEncrypted,
                clearLegacyPlain = { DevelopConfig.putAppSecret("") },
            )
        ) {
            migratedCount++
        } else if (!DevelopConfig.getAppSecret().isNullOrBlank()) {
            failedCount++
        }

        return PlaintextMigrationResult(
            migratedCount = migratedCount,
            failedCount = failedCount,
        )
    }

    fun getAppId(): String? {
        if (isBuildCredentialInjected()) {
            return BuildConfig.DANDAN_APP_ID.trim().takeIf { it.isNotEmpty() }
        }

        return getCredential(
            encrypted = DevelopConfig.getAppIdEncrypted(),
            legacyPlain = DevelopConfig.getAppId(),
            saveEncrypted = DevelopConfig::putAppIdEncrypted,
            clearLegacyPlain = { DevelopConfig.putAppId("") },
        )
    }

    fun getAppSecret(): String? {
        if (isBuildCredentialInjected()) {
            return BuildConfig.DANDAN_APP_SECRET.trim().takeIf { it.isNotEmpty() }
        }

        return getCredential(
            encrypted = DevelopConfig.getAppSecretEncrypted(),
            legacyPlain = DevelopConfig.getAppSecret(),
            saveEncrypted = DevelopConfig::putAppSecretEncrypted,
            clearLegacyPlain = { DevelopConfig.putAppSecret("") },
        )
    }

    /**
     * 仅用于认证输入框回显：只回显本地保存的值，不回显编译期注入的值，避免在UI里直接暴露。
     */
    fun getStoredAppIdForPrefill(): String? =
        getStoredCredentialForPrefill(
            encrypted = DevelopConfig.getAppIdEncrypted(),
            legacyPlain = DevelopConfig.getAppId(),
        )

    /**
     * 仅用于认证输入框回显：只回显本地保存的值，不回显编译期注入的值，避免在UI里直接暴露。
     */
    fun getStoredAppSecretForPrefill(): String? =
        getStoredCredentialForPrefill(
            encrypted = DevelopConfig.getAppSecretEncrypted(),
            legacyPlain = DevelopConfig.getAppSecret(),
        )

    fun putAppId(appId: String) {
        putCredential(
            value = appId,
            saveEncrypted = DevelopConfig::putAppIdEncrypted,
            clearLegacyPlain = { DevelopConfig.putAppId("") },
            saveLegacyPlain = DevelopConfig::putAppId,
        )
    }

    fun putAppSecret(appSecret: String) {
        putCredential(
            value = appSecret,
            saveEncrypted = DevelopConfig::putAppSecretEncrypted,
            clearLegacyPlain = { DevelopConfig.putAppSecret("") },
            saveLegacyPlain = DevelopConfig::putAppSecret,
        )
    }

    fun putCredentials(
        appId: String,
        appSecret: String
    ): SaveResult {
        val appIdValue = appId.trim()
        val appSecretValue = appSecret.trim()

        val appIdEncrypted = appIdValue.takeIf { it.isNotBlank() }?.let(CredentialCrypto::encrypt)
        val appSecretEncrypted = appSecretValue.takeIf { it.isNotBlank() }?.let(CredentialCrypto::encrypt)

        val hasEncryptFailure =
            (appIdValue.isNotBlank() && appIdEncrypted == null) ||
                (appSecretValue.isNotBlank() && appSecretEncrypted == null)

        if (hasEncryptFailure && !isPlaintextFallbackEnabled()) {
            return SaveResult.REJECTED_ENCRYPTION_FAILED
        }

        commitCredential(
            value = appIdValue,
            encrypted = appIdEncrypted,
            saveEncrypted = DevelopConfig::putAppIdEncrypted,
            clearLegacyPlain = { DevelopConfig.putAppId("") },
            saveLegacyPlain = DevelopConfig::putAppId,
        )
        commitCredential(
            value = appSecretValue,
            encrypted = appSecretEncrypted,
            saveEncrypted = DevelopConfig::putAppSecretEncrypted,
            clearLegacyPlain = { DevelopConfig.putAppSecret("") },
            saveLegacyPlain = DevelopConfig::putAppSecret,
        )

        return if (hasEncryptFailure) {
            SaveResult.SAVED_WITH_PLAINTEXT_FALLBACK
        } else {
            SaveResult.SAVED_ENCRYPTED
        }
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
        val trimmedValue = value.trim()
        val encrypted = trimmedValue.takeIf { it.isNotBlank() }?.let(CredentialCrypto::encrypt)
        commitCredential(
            value = trimmedValue,
            encrypted = encrypted,
            saveEncrypted = saveEncrypted,
            clearLegacyPlain = clearLegacyPlain,
            saveLegacyPlain = saveLegacyPlain,
        )
    }

    private fun commitCredential(
        value: String,
        encrypted: String?,
        saveEncrypted: (String) -> Unit,
        clearLegacyPlain: () -> Unit,
        saveLegacyPlain: (String) -> Unit
    ) {
        if (value.isBlank()) {
            saveEncrypted("")
            clearLegacyPlain()
            return
        }

        if (encrypted != null) {
            saveEncrypted(encrypted)
            clearLegacyPlain()
            return
        }

        if (isPlaintextFallbackEnabled()) {
            saveLegacyPlain(value)
            saveEncrypted("")
        }
    }

    private fun migrateSingleLegacyCredential(
        legacyPlain: String?,
        saveEncrypted: (String) -> Unit,
        clearLegacyPlain: () -> Unit
    ): Boolean {
        val value = legacyPlain?.trim().orEmpty()
        if (value.isBlank()) {
            return false
        }

        val encrypted = CredentialCrypto.encrypt(value) ?: return false
        saveEncrypted(encrypted)
        clearLegacyPlain()
        return true
    }
}

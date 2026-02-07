package com.xyoye.common_component.preference

import androidx.preference.PreferenceDataStore
import com.xyoye.common_component.utils.ErrorReportHelper

open class MappingPreferenceDataStore(
    private val dataStoreName: String,
    private val booleanReaders: Map<String, () -> Boolean> = emptyMap(),
    private val booleanWriters: Map<String, (Boolean) -> Unit> = emptyMap(),
    private val stringReaders: Map<String, () -> String?> = emptyMap(),
    private val stringWriters: Map<String, (String?) -> Unit> = emptyMap(),
    private val intReaders: Map<String, () -> Int> = emptyMap(),
    private val intWriters: Map<String, (Int) -> Unit> = emptyMap(),
) : PreferenceDataStore() {
    override fun getBoolean(
        key: String?,
        defValue: Boolean
    ): Boolean {
        val resolvedKey = key ?: return super.getBoolean(key, defValue)
        val reader = booleanReaders[resolvedKey] ?: return super.getBoolean(key, defValue)
        return read(
            method = "getBoolean",
            key = resolvedKey,
            defaultValue = defValue,
            action = reader,
        )
    }

    override fun putBoolean(
        key: String?,
        value: Boolean
    ) {
        val resolvedKey = key ?: return super.putBoolean(key, value)
        val writer = booleanWriters[resolvedKey] ?: return
        write(
            method = "putBoolean",
            key = resolvedKey,
        ) {
            writer(value)
        }
    }

    override fun getString(
        key: String?,
        defValue: String?
    ): String? {
        val resolvedKey = key ?: return super.getString(key, defValue)
        val reader = stringReaders[resolvedKey] ?: return super.getString(key, defValue)
        return read(
            method = "getString",
            key = resolvedKey,
            defaultValue = defValue,
            action = reader,
        )
    }

    override fun putString(
        key: String?,
        value: String?
    ) {
        val resolvedKey = key ?: return super.putString(key, value)
        val writer = stringWriters[resolvedKey] ?: return
        write(
            method = "putString",
            key = resolvedKey,
        ) {
            writer(value)
        }
    }

    override fun getInt(
        key: String?,
        defValue: Int
    ): Int {
        val resolvedKey = key ?: return super.getInt(key, defValue)
        val reader = intReaders[resolvedKey] ?: return super.getInt(key, defValue)
        return read(
            method = "getInt",
            key = resolvedKey,
            defaultValue = defValue,
            action = reader,
        )
    }

    override fun putInt(
        key: String?,
        value: Int
    ) {
        val resolvedKey = key ?: return super.putInt(key, value)
        val writer = intWriters[resolvedKey] ?: return
        write(
            method = "putInt",
            key = resolvedKey,
        ) {
            writer(value)
        }
    }

    private inline fun <T> read(
        method: String,
        key: String,
        defaultValue: T,
        action: () -> T,
    ): T =
        try {
            action()
        } catch (exception: Exception) {
            reportException(
                exception = exception,
                method = method,
                key = key,
            )
            defaultValue
        }

    private inline fun write(
        method: String,
        key: String,
        action: () -> Unit,
    ) {
        try {
            action()
        } catch (exception: Exception) {
            reportException(
                exception = exception,
                method = method,
                key = key,
            )
        }
    }

    private fun reportException(
        exception: Exception,
        method: String,
        key: String,
    ) {
        runCatching {
            ErrorReportHelper.postCatchedExceptionWithContext(
                exception,
                dataStoreName,
                method,
                "Failed to handle preference value for key: $key",
            )
        }
    }
}

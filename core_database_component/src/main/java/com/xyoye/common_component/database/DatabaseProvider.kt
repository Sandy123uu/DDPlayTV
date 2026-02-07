package com.xyoye.common_component.database

import androidx.annotation.VisibleForTesting

fun interface DatabaseAccessProvider {
    fun provide(): DatabaseInfo
}

object DatabaseProvider {
    private val defaultProvider = DatabaseAccessProvider { DatabaseManager.instance }

    @Volatile
    private var provider: DatabaseAccessProvider = defaultProvider

    val instance: DatabaseInfo
        get() = provider.provide()

    @VisibleForTesting
    fun overrideProviderForTest(testProvider: DatabaseAccessProvider?) {
        provider = testProvider ?: defaultProvider
    }
}

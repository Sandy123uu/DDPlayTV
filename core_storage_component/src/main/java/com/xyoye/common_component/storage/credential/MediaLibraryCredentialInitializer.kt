package com.xyoye.common_component.storage.credential

import android.content.Context
import androidx.startup.Initializer
import com.xyoye.common_component.base.app.BaseInitializer

class MediaLibraryCredentialInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        MediaLibraryCredentialStore.scheduleMigration()
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf(BaseInitializer::class.java)
}

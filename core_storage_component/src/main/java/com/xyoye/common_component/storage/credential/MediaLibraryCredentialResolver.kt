package com.xyoye.common_component.storage.credential

import com.xyoye.data_component.entity.MediaLibraryEntity

object MediaLibraryCredentialResolver {
    fun resolve(library: MediaLibraryEntity): MediaLibraryEntity {
        val libraryId = library.id
        if (libraryId <= 0) return library

        val password = library.password?.takeIf { it.isNotBlank() } ?: MediaLibraryCredentialStore.readPassword(libraryId)
        val remoteSecret =
            library.remoteSecret?.takeIf { it.isNotBlank() }
                ?: MediaLibraryCredentialStore.readRemoteSecret(libraryId)

        if (password == library.password && remoteSecret == library.remoteSecret) {
            return library
        }

        return library.copy(password = password, remoteSecret = remoteSecret)
    }
}

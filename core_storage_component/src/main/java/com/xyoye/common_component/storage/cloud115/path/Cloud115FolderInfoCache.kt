package com.xyoye.common_component.storage.cloud115.path

import com.xyoye.common_component.network.repository.Cloud115Repository
import java.util.concurrent.ConcurrentHashMap

internal class Cloud115FolderInfoCache(
    private val repository: Cloud115Repository
) {
    private val breadcrumbIdCache = ConcurrentHashMap<String, List<String>>()

    suspend fun resolveBreadcrumbIds(folderId: String): List<String> {
        val folder = folderId.trim()
        if (folder.isBlank() || folder == ROOT_CID) {
            return emptyList()
        }

        breadcrumbIdCache[folder]?.let { return it }

        val breadcrumb = fetchBreadcrumbIds(folder)
        for (index in breadcrumb.indices) {
            val id = breadcrumb[index]
            breadcrumbIdCache.putIfAbsent(id, breadcrumb.subList(0, index + 1).toList())
        }

        return breadcrumbIdCache[folder].orEmpty()
    }

    private suspend fun fetchBreadcrumbIds(folderId: String): List<String> {
        val response = repository.stat(cid = folderId).getOrThrow()
        val ids =
            response.paths.orEmpty()
                .mapNotNull { it.fileId?.toString()?.trim() }
                .filter { it.isNotBlank() && it != ROOT_CID }

        if (ids.isEmpty()) {
            return listOf(folderId)
        }

        return if (ids.last() == folderId) ids else ids + folderId
    }

    companion object {
        private const val ROOT_CID: String = "0"
    }
}


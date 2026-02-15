package com.xyoye.common_component.log.http.page

import android.content.Context
import com.xyoye.core_log_component.R
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream

class HttpLogPageHandler(
    private val context: Context,
) {
    private val lock = Any()

    @Volatile
    private var cachedBytes: ByteArray? = null

    fun handle(): NanoHTTPD.Response {
        val bytes = loadBytes()
        val response =
            NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "text/html; charset=utf-8",
                ByteArrayInputStream(bytes),
                bytes.size.toLong(),
            )
        response.addHeader("Cache-Control", "no-store")
        return response
    }

    private fun loadBytes(): ByteArray {
        val existing = cachedBytes
        if (existing != null) return existing
        synchronized(lock) {
            val second = cachedBytes
            if (second != null) return second
            val loaded =
                context.resources.openRawResource(R.raw.http_log_index).use { it.readBytes() }
            cachedBytes = loaded
            return loaded
        }
    }
}


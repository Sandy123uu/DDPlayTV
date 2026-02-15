package com.xyoye.common_component.log.http.logcat

import android.os.Build
import android.os.Process as AndroidProcess
import java.io.BufferedReader
import java.io.InputStreamReader

class LogcatCollector(
    private val onLine: (String) -> Unit,
) {
    private val lock = Any()

    @Volatile
    private var process: Process? = null

    @Volatile
    private var readerThread: Thread? = null

    @Volatile
    private var available: Boolean = false

    @Volatile
    private var lastError: String? = null

    fun start(): Boolean {
        synchronized(lock) {
            stopLocked()
            val pid = AndroidProcess.myPid()
            val builder =
                ProcessBuilder(
                    "logcat",
                    "--pid=$pid",
                    "-v",
                    "epoch",
                ).redirectErrorStream(true)

            val newProcess =
                runCatching { builder.start() }.getOrElse { error ->
                    available = false
                    lastError = error.message ?: error::class.java.simpleName
                    return false
                }
            process = newProcess
            available = true
            lastError = null

            val thread =
                Thread(
                    {
                        readLoop(newProcess)
                    },
                    "HttpLogcatCollector",
                ).apply { isDaemon = true }
            readerThread = thread
            thread.start()
            return true
        }
    }

    fun stop() {
        synchronized(lock) {
            stopLocked()
        }
    }

    fun isAvailable(): Boolean = available

    fun lastError(): String? = lastError

    private fun stopLocked() {
        available = false
        val current = process
        if (current != null) {
            runCatching { current.destroy() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { current.destroyForcibly() }
            }
        }
        process = null
        readerThread = null
    }

    private fun readLoop(process: Process) {
        runCatching {
            BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8)).useLines { lines ->
                lines.forEach { line ->
                    if (!available) return
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        onLine(trimmed)
                    }
                }
            }
        }.onFailure { error ->
            available = false
            lastError = error.message ?: error::class.java.simpleName
        }
    }
}

package com.xyoye.player.controller.setting

/**
 * Coalesces frequent max-line updates and commits only the latest value.
 */
internal class DanmuMaxLineCommitCoordinator(
    private val debounceMillis: Long,
    private val scheduler: Scheduler,
    private val commitAction: (mode: Int, line: Int) -> Unit
) {
    private data class ModeState(
        var currentLine: Int,
        var committedLine: Int
    )

    private val modeStateMap = mutableMapOf<Int, ModeState>()
    private var pendingMode: Int? = null
    private var pendingTask: Cancelable? = null

    fun request(
        mode: Int,
        currentLine: Int,
        line: Int,
        debounce: Boolean
    ): Boolean {
        val state = getModeState(mode, currentLine)
        if (state.currentLine == line) {
            return false
        }
        state.currentLine = line

        if (state.currentLine == state.committedLine) {
            if (pendingMode == mode) {
                clearPendingTask()
            }
            return true
        }

        if (debounce) {
            schedule(mode)
        } else {
            commitMode(mode)
        }
        return true
    }

    fun flush() {
        pendingMode?.let { mode ->
            commitMode(mode)
        }
    }

    fun flushMode(mode: Int) {
        if (pendingMode == mode) {
            commitMode(mode)
            return
        }
        val state = modeStateMap[mode] ?: return
        if (state.currentLine != state.committedLine) {
            commitMode(mode)
        }
    }

    fun cancelPending() {
        clearPendingTask()
    }

    private fun schedule(mode: Int) {
        if (pendingMode != null && pendingMode != mode) {
            flush()
        }
        clearPendingTask()
        pendingMode = mode
        pendingTask =
            scheduler.postDelayed(debounceMillis) {
                val targetMode = pendingMode ?: return@postDelayed
                clearPendingTask()
                commitMode(targetMode)
            }
    }

    private fun commitMode(mode: Int) {
        val state = modeStateMap[mode] ?: return
        if (pendingMode == mode) {
            clearPendingTask()
        }
        if (state.currentLine == state.committedLine) {
            return
        }
        state.committedLine = state.currentLine
        commitAction(mode, state.currentLine)
    }

    private fun clearPendingTask() {
        pendingTask?.cancel()
        pendingTask = null
        pendingMode = null
    }

    private fun getModeState(
        mode: Int,
        currentLine: Int
    ): ModeState =
        modeStateMap.getOrPut(mode) {
            ModeState(
                currentLine = currentLine,
                committedLine = currentLine
            )
        }

    internal interface Scheduler {
        fun postDelayed(
            delayMillis: Long,
            action: () -> Unit
        ): Cancelable
    }

    internal fun interface Cancelable {
        fun cancel()
    }
}

package com.xyoye.player.controller.setting

import org.junit.Assert.assertEquals
import org.junit.Test

class DanmuMaxLineCommitCoordinatorTest {
    @Test
    fun debouncedRequestsCommitOnlyLatestValue() {
        val scheduler = FakeScheduler()
        val commits = mutableListOf<CommitRecord>()
        val coordinator =
            DanmuMaxLineCommitCoordinator(
                debounceMillis = DEBOUNCE_MILLIS,
                scheduler = scheduler,
                commitAction = { mode, line -> commits += CommitRecord(mode, line) }
            )

        coordinator.request(mode = MODE_SCROLL, currentLine = 0, line = 2, debounce = true)
        coordinator.request(mode = MODE_SCROLL, currentLine = 2, line = 3, debounce = true)

        scheduler.advanceBy(DEBOUNCE_MILLIS - 1)
        assertEquals(emptyList<CommitRecord>(), commits)

        scheduler.advanceBy(1)
        assertEquals(listOf(CommitRecord(MODE_SCROLL, 3)), commits)
    }

    @Test
    fun flushCommitsPendingChangeImmediately() {
        val scheduler = FakeScheduler()
        val commits = mutableListOf<CommitRecord>()
        val coordinator =
            DanmuMaxLineCommitCoordinator(
                debounceMillis = DEBOUNCE_MILLIS,
                scheduler = scheduler,
                commitAction = { mode, line -> commits += CommitRecord(mode, line) }
            )

        coordinator.request(mode = MODE_TOP, currentLine = 0, line = 4, debounce = true)
        coordinator.flush()

        assertEquals(listOf(CommitRecord(MODE_TOP, 4)), commits)
        scheduler.advanceBy(DEBOUNCE_MILLIS)
        assertEquals(listOf(CommitRecord(MODE_TOP, 4)), commits)
    }

    @Test
    fun sameValueDoesNotCommitRepeatedly() {
        val scheduler = FakeScheduler()
        val commits = mutableListOf<CommitRecord>()
        val coordinator =
            DanmuMaxLineCommitCoordinator(
                debounceMillis = DEBOUNCE_MILLIS,
                scheduler = scheduler,
                commitAction = { mode, line -> commits += CommitRecord(mode, line) }
            )

        coordinator.request(mode = MODE_BOTTOM, currentLine = 0, line = 6, debounce = true)
        scheduler.advanceBy(DEBOUNCE_MILLIS)
        assertEquals(listOf(CommitRecord(MODE_BOTTOM, 6)), commits)

        coordinator.request(mode = MODE_BOTTOM, currentLine = 6, line = 6, debounce = true)
        scheduler.advanceBy(DEBOUNCE_MILLIS)
        assertEquals(listOf(CommitRecord(MODE_BOTTOM, 6)), commits)
    }

    @Test
    fun flushModeCommitsPerModeWithoutCrossWriting() {
        val scheduler = FakeScheduler()
        val commits = mutableListOf<CommitRecord>()
        val coordinator =
            DanmuMaxLineCommitCoordinator(
                debounceMillis = DEBOUNCE_MILLIS,
                scheduler = scheduler,
                commitAction = { mode, line -> commits += CommitRecord(mode, line) }
            )

        coordinator.request(mode = MODE_SCROLL, currentLine = 0, line = 2, debounce = true)
        coordinator.flushMode(MODE_SCROLL)

        coordinator.request(mode = MODE_TOP, currentLine = 0, line = 5, debounce = true)
        scheduler.advanceBy(DEBOUNCE_MILLIS)

        assertEquals(
            listOf(
                CommitRecord(MODE_SCROLL, 2),
                CommitRecord(MODE_TOP, 5)
            ),
            commits
        )
    }

    private data class CommitRecord(
        val mode: Int,
        val line: Int
    )

    private class FakeScheduler : DanmuMaxLineCommitCoordinator.Scheduler {
        private data class Task(
            val executeAt: Long,
            val action: () -> Unit,
            var canceled: Boolean = false
        )

        private var nowMillis: Long = 0
        private val tasks = mutableListOf<Task>()

        override fun postDelayed(
            delayMillis: Long,
            action: () -> Unit
        ): DanmuMaxLineCommitCoordinator.Cancelable {
            val task = Task(executeAt = nowMillis + delayMillis, action = action)
            tasks += task
            return DanmuMaxLineCommitCoordinator.Cancelable {
                task.canceled = true
            }
        }

        fun advanceBy(durationMillis: Long) {
            nowMillis += durationMillis
            runDueTasks()
        }

        private fun runDueTasks() {
            while (true) {
                val task =
                    tasks
                        .filter { !it.canceled && it.executeAt <= nowMillis }
                        .minByOrNull { it.executeAt }
                        ?: break
                tasks.remove(task)
                task.action()
            }
            tasks.removeAll { it.canceled }
        }
    }

    private companion object {
        private const val DEBOUNCE_MILLIS = 180L
        private const val MODE_SCROLL = 1
        private const val MODE_TOP = 5
        private const val MODE_BOTTOM = 6
    }
}

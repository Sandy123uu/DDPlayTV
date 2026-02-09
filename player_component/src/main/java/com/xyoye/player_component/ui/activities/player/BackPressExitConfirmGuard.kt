package com.xyoye.player_component.ui.activities.player

internal class BackPressExitConfirmGuard(
    private val confirmWindowMs: Long,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    private var lastBackPressedAtMs: Long = 0L

    fun shouldInterceptExit(): Boolean {
        val now = nowProvider()
        if (now - lastBackPressedAtMs > confirmWindowMs) {
            lastBackPressedAtMs = now
            return true
        }
        return false
    }

    fun reset() {
        lastBackPressedAtMs = 0L
    }
}

package com.xyoye.common_component.focus

import android.view.KeyEvent
import android.view.View
import androidx.annotation.MainThread
import com.google.android.material.tabs.TabLayout
import com.xyoye.common_component.extension.isTelevisionUiMode

/**
 * TV/非触摸（DPAD）模式下的通用输入/焦点策略。
 */
@MainThread
fun View.applyDpadEditTextPolicy(enabled: Boolean = context.isTelevisionUiMode() && !isInTouchMode) {
    if (!enabled) return

    clearFocus()
    isFocusable = false
    isFocusableInTouchMode = false
}

/**
 * 将某个“标题/搜索栏容器”的 DPAD_DOWN 行为桥接到 Tab 行（常用于 TabLayout 上方的控件）。
 */
@MainThread
fun View.bindDpadDownToTabFocus(
    coordinator: TabLayoutViewPager2DpadFocusCoordinator,
    tabLayout: TabLayout,
    enabled: () -> Boolean = { context.isTelevisionUiMode() && !tabLayout.isInTouchMode }
) {
    setOnKeyListener { _, keyCode, event ->
        if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
        if (!enabled()) return@setOnKeyListener false

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> coordinator.requestTabFocus()
            else -> false
        }
    }
}

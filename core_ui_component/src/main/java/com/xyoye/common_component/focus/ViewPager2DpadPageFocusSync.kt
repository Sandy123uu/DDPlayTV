package com.xyoye.common_component.focus

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * ViewPager2 在 D-Pad/TV 场景下的“焦点 -> 页面”同步：
 * - 当焦点落在非当前页的子 View 上时，自动切换到对应页面。
 * - 解决 ViewPager2 离屏页面仍在层级中导致的“看见的是A页，按确定却触发B页条目”的问题。
 */
class ViewPager2DpadPageFocusSync(
    private val viewPager: ViewPager2,
    private val isEnabled: () -> Boolean = { !viewPager.isInTouchMode },
    private val smoothScroll: Boolean = true
) {
    private var isHandling = false

    private val focusChangeListener =
        ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
            if (!isEnabled()) return@OnGlobalFocusChangeListener
            val focus = newFocus ?: return@OnGlobalFocusChangeListener
            syncToFocusedView(focus)
        }

    @MainThread
    fun attach() {
        viewPager.viewTreeObserver.addOnGlobalFocusChangeListener(focusChangeListener)
    }

    @MainThread
    fun detach() {
        viewPager.viewTreeObserver.removeOnGlobalFocusChangeListener(focusChangeListener)
    }

    private fun syncToFocusedView(focus: View) {
        if (isHandling) return
        val recyclerView = recyclerView() ?: return
        if (!isDescendantOf(focus, recyclerView)) return

        val pageRoot = findDirectChildUnderRecyclerView(focus, recyclerView) ?: return
        val position = recyclerView.getChildAdapterPosition(pageRoot)
        if (position == RecyclerView.NO_POSITION) return
        if (position == viewPager.currentItem) return

        isHandling = true
        try {
            viewPager.setCurrentItem(position, smoothScroll)
        } finally {
            isHandling = false
        }
    }

    private fun recyclerView(): RecyclerView? = viewPager.getChildAt(0) as? RecyclerView

    private fun findDirectChildUnderRecyclerView(
        view: View,
        recyclerView: RecyclerView
    ): View? {
        var current: View? = view
        while (current != null) {
            val parent = current.parent
            if (parent === recyclerView) {
                return current
            }
            current = parent as? View
        }
        return null
    }

    private fun isDescendantOf(
        view: View,
        ancestor: View
    ): Boolean {
        var current: View? = view
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent as? View
        }
        return false
    }
}


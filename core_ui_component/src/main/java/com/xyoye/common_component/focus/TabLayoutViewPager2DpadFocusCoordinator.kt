package com.xyoye.common_component.focus

import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.MainThread
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.xyoye.common_component.extension.requestIndexChildFocus
import com.xyoye.common_component.extension.toResString
import com.xyoye.core_ui_component.R
import java.lang.ref.WeakReference

/**
 * TabLayout + ViewPager2 在 D-Pad/TV 场景下的焦点协调：
 * - Tab 可聚焦且 focused 可见（样式由 Tab background selector 提供）
 * - 从 Tab 行按下进入内容：聚焦到当前页首要可操作控件（或恢复页内上次焦点）
 * - 从内容顶部按上返回 Tab 行
 * - 整合 ViewPager2DpadPageFocusSync，避免离屏页误触发
 *
 * 注意：建议只在 “TV UI mode 且非触摸模式” 下启用（通过 isEnabled 控制）。
 */
class TabLayoutViewPager2DpadFocusCoordinator(
    private val tabLayout: TabLayout,
    private val viewPager: ViewPager2,
    private val isEnabled: () -> Boolean = { !tabLayout.isInTouchMode }
) {
    private val pageFocusSync =
        ViewPager2DpadPageFocusSync(
            viewPager = viewPager,
            isEnabled = isEnabled,
        )

    private val lastFocusedViewByPage = SparseArray<WeakReference<View>>()

    private val focusChangeListener =
        ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
            if (!isEnabled()) return@OnGlobalFocusChangeListener
            val focus = newFocus ?: return@OnGlobalFocusChangeListener
            recordLastFocus(focus)
        }

    private val tabViewKeyListener =
        View.OnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@OnKeyListener false
            if (!isEnabled()) return@OnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> requestFocusToCurrentPage()
                else -> false
            }
        }

    private val contentKeyListener =
        View.OnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@OnKeyListener false
            if (!isEnabled()) return@OnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> handleDpadUpFromContent()
                else -> false
            }
        }

    @MainThread
    fun attach() {
        viewPager.setOnKeyListener(contentKeyListener)
        viewPager.viewTreeObserver.addOnGlobalFocusChangeListener(focusChangeListener)
        pageFocusSync.attach()
        updateTabViews()
        tabLayout.post { updateTabViews() }
    }

    @MainThread
    fun detach() {
        pageFocusSync.detach()
        viewPager.viewTreeObserver.removeOnGlobalFocusChangeListener(focusChangeListener)
        viewPager.setOnKeyListener(null)
        clearTabViewListeners()
    }

    @MainThread
    fun requestTabFocus(): Boolean {
        if (!isEnabled()) return false
        val tabStrip = tabStrip() ?: return false
        val index = viewPager.currentItem
        val tabView = tabStrip.getChildAt(index) ?: return false
        return tabView.requestFocus()
    }

    private fun handleDpadUpFromContent(): Boolean {
        val focus = viewPager.findFocus() ?: return false
        val pageRoot = currentPageItemView() ?: return false
        if (!isDescendantOf(focus, pageRoot)) return false

        val recyclerView = findNearestRecyclerView(focus, stopAt = pageRoot) ?: return false
        if (recyclerView.canScrollVertically(-1)) {
            return false
        }

        val next = focus.focusSearch(View.FOCUS_UP)
        if (next != null && isDescendantOf(next, recyclerView)) {
            return false
        }

        return requestTabFocus()
    }

    private fun requestFocusToCurrentPage(): Boolean {
        val index = viewPager.currentItem
        val lastFocused = lastFocusedViewByPage.get(index)?.get()
        if (lastFocused != null && lastFocused.isShown && lastFocused.isFocusable && lastFocused.isAttachedToWindow) {
            return lastFocused.requestFocus()
        }

        val pageRoot = currentPageItemView()
        if (pageRoot == null) {
            viewPager.post { requestFocusToCurrentPage() }
            return true
        }

        val focusableTag = R.string.focusable_item.toResString()
        val tagged = pageRoot.findViewWithTag<View>(focusableTag)
        if (tagged != null && tagged.isShown && tagged.isFocusable) {
            return tagged.requestFocus()
        }

        val recyclerView = findFirstRecyclerView(pageRoot)
        if (recyclerView != null) {
            val firstFocusable = findFirstFocusableChildInRecyclerView(recyclerView)
            if (firstFocusable != null && firstFocusable.requestFocus()) {
                return true
            }

            if (recyclerView.requestIndexChildFocus(0)) {
                return true
            }
            return recyclerView.requestFocus()
        }

        val firstFocusable = findFirstFocusableDescendant(pageRoot) ?: return false
        return firstFocusable.requestFocus()
    }

    private fun recordLastFocus(focus: View) {
        val viewPagerRv = viewPagerRecyclerView() ?: return
        if (!isDescendantOf(focus, viewPagerRv)) return

        val pageRoot = findDirectChildUnderRecyclerView(focus, viewPagerRv) ?: return
        val pageIndex = viewPagerRv.getChildAdapterPosition(pageRoot)
        if (pageIndex == RecyclerView.NO_POSITION) return
        lastFocusedViewByPage.put(pageIndex, WeakReference(focus))
    }

    private fun updateTabViews() {
        val tabStrip = tabStrip() ?: return
        tabStrip.children.forEach { tabView ->
            tabView.applyDpadFocusable(enabled = true, inTouchMode = tabView.isInTouchMode)
            tabView.setOnKeyListener(tabViewKeyListener)
        }
    }

    private fun clearTabViewListeners() {
        val tabStrip = tabStrip() ?: return
        tabStrip.children.forEach { tabView ->
            tabView.setOnKeyListener(null)
        }
    }

    private fun tabStrip(): ViewGroup? = tabLayout.getChildAt(0) as? ViewGroup

    private fun viewPagerRecyclerView(): RecyclerView? = viewPager.getChildAt(0) as? RecyclerView

    private fun currentPageItemView(): View? {
        val rv = viewPagerRecyclerView() ?: return null
        val index = viewPager.currentItem
        return rv.layoutManager?.findViewByPosition(index)
            ?: rv.findViewHolderForAdapterPosition(index)?.itemView
    }

    private fun findFirstRecyclerView(root: View): RecyclerView? {
        if (root is RecyclerView) return root
        val vg = root as? ViewGroup ?: return null
        vg.children.forEach { child ->
            val found = findFirstRecyclerView(child)
            if (found != null) return found
        }
        return null
    }

    private fun findNearestRecyclerView(
        view: View,
        stopAt: View
    ): RecyclerView? {
        var current: View? = view
        while (current != null && current !== stopAt) {
            val parent = current.parent as? View ?: return null
            if (parent is RecyclerView) {
                return parent
            }
            current = parent
        }
        return null
    }

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

    private fun findFirstFocusableChildInRecyclerView(recyclerView: RecyclerView): View? {
        if (recyclerView.isInTouchMode) return null

        val focusableTag = R.string.focusable_item.toResString()
        recyclerView.children.forEach { child ->
            val tagged = child.findViewWithTag<View>(focusableTag)
            if (tagged != null && tagged.isShown && tagged.isFocusable) {
                return tagged
            }

            val found = findFirstFocusableDescendant(child)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun findFirstFocusableDescendant(root: View): View? {
        if (root.isShown && root.isFocusable) {
            return root
        }
        val vg = root as? ViewGroup ?: return null
        vg.children.forEach { child ->
            val found = findFirstFocusableDescendant(child)
            if (found != null) return found
        }
        return null
    }
}

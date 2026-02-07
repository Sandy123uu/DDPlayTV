package com.xyoye.common_component.focus

import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.MainThread
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.xyoye.common_component.extension.isTelevisionUiMode
import com.xyoye.common_component.extension.requestIndexChildFocus
import com.xyoye.common_component.extension.toResString
import com.xyoye.core_ui_component.R
import java.lang.ref.WeakReference

enum class TabDpadMode {
    /**
     * 默认模式：Tab 行可聚焦，并通过 DPAD_UP/DOWN 在 Tab 与内容之间切换。
     */
    Default,

    /**
     * 设置页模式：Tab 行仅作选中指示（TV/非触摸模式下不可聚焦），内容内通过 DPAD_LEFT/RIGHT 切页。
     */
    SettingsIndicatorOnly
}

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
    private val mode: TabDpadMode = TabDpadMode.Default,
    private val isEnabled: () -> Boolean = { !tabLayout.isInTouchMode }
) {
    companion object {
        /**
         * 在 TV UI mode 下自动 attach，并返回已 attach 的 coordinator；非 TV 设备返回 null。
         *
         * 注意：默认只在“非触摸模式”下启用（`!tabLayout.isInTouchMode`），以避免误伤移动端触控交互。
         */
        @MainThread
        fun attachIfTelevision(
            tabLayout: TabLayout,
            viewPager: ViewPager2,
            mode: TabDpadMode = TabDpadMode.Default,
            isEnabled: (() -> Boolean)? = null
        ): TabLayoutViewPager2DpadFocusCoordinator? {
            if (!tabLayout.context.isTelevisionUiMode()) {
                return null
            }

            return TabLayoutViewPager2DpadFocusCoordinator(
                tabLayout = tabLayout,
                viewPager = viewPager,
                mode = mode,
                isEnabled = isEnabled ?: { !tabLayout.isInTouchMode },
            ).also { it.attach() }
        }
    }

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
                KeyEvent.KEYCODE_DPAD_UP ->
                    if (mode == TabDpadMode.Default) {
                        handleDpadUpFromContent()
                    } else {
                        false
                    }
                else -> false
            }
        }

    private val contentUnhandledKeyListener =
        ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@OnUnhandledKeyEventListenerCompat false
            if (!isEnabled()) return@OnUnhandledKeyEventListenerCompat false
            if (mode != TabDpadMode.SettingsIndicatorOnly) return@OnUnhandledKeyEventListenerCompat false

            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> trySwitchPageByDpad(event.keyCode)

                else -> false
            }
        }

    @MainThread
    fun attach() {
        viewPager.setOnKeyListener(contentKeyListener)
        ViewCompat.addOnUnhandledKeyEventListener(viewPager, contentUnhandledKeyListener)
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
        ViewCompat.removeOnUnhandledKeyEventListener(viewPager, contentUnhandledKeyListener)
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

    @MainThread
    fun requestContentFocus(): Boolean {
        if (!isEnabled()) return false
        return requestFocusToPage(pageIndex = viewPager.currentItem)
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

    private fun requestFocusToCurrentPage(): Boolean = requestFocusToPage(pageIndex = viewPager.currentItem)

    private fun requestFocusToPage(pageIndex: Int): Boolean {
        val lastFocused = lastFocusedViewByPage.get(pageIndex)?.get()
        if (lastFocused != null && lastFocused.isShown && lastFocused.isFocusable && lastFocused.isAttachedToWindow) {
            return lastFocused.requestFocus()
        }

        val pageRoot = pageItemView(pageIndex)
        if (pageRoot == null) {
            viewPager.post { requestFocusToPage(pageIndex) }
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

    private fun trySwitchPageByDpad(keyCode: Int): Boolean {
        val focus = viewPager.findFocus() ?: return false
        val currentPageRoot = currentPageItemView() ?: return false
        if (!isDescendantOf(focus, currentPageRoot)) return false

        val itemCount = viewPager.adapter?.itemCount ?: return false
        val current = viewPager.currentItem
        val target =
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> current - 1
                KeyEvent.KEYCODE_DPAD_RIGHT -> current + 1
                else -> return false
            }

        if (target !in 0 until itemCount) {
            return false
        }

        viewPager.setCurrentItem(target, true)
        return requestFocusToPage(pageIndex = target)
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
        val isTabDpadFocusable = mode == TabDpadMode.Default || !isEnabled()

        tabStrip.children.forEach { tabView ->
            tabView.applyDpadFocusable(
                enabled = isTabDpadFocusable,
                inTouchMode = tabView.isInTouchMode,
            )
            tabView.setOnKeyListener(if (isTabDpadFocusable) tabViewKeyListener else null)
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

    private fun currentPageItemView(): View? = pageItemView(pageIndex = viewPager.currentItem)

    private fun pageItemView(pageIndex: Int): View? {
        val rv = viewPagerRecyclerView() ?: return null
        return rv.layoutManager?.findViewByPosition(pageIndex)
            ?: rv.findViewHolderForAdapterPosition(pageIndex)?.itemView
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

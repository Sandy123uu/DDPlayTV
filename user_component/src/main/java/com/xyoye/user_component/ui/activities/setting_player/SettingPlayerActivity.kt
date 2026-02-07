package com.xyoye.user_component.ui.activities.setting_player

import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.alibaba.android.arouter.facade.annotation.Route
import com.google.android.material.tabs.TabLayoutMediator
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.extension.isTelevisionUiMode
import com.xyoye.common_component.focus.TabDpadMode
import com.xyoye.common_component.focus.TabLayoutViewPager2DpadFocusCoordinator
import com.xyoye.user_component.BR
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.ActivitySettingPlayerBinding
import com.xyoye.user_component.ui.fragment.DanmuSettingFragment
import com.xyoye.user_component.ui.fragment.PlayerSettingFragment
import com.xyoye.user_component.ui.fragment.SubtitleSettingFragment

@Route(path = RouteTable.User.SettingPlayer)
class SettingPlayerActivity : BaseActivity<SettingPlayerViewModel, ActivitySettingPlayerBinding>() {
    private val pageAdapter by lazy { SettingFragmentAdapter() }
    private var tvFocusCoordinator: TabLayoutViewPager2DpadFocusCoordinator? = null

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            SettingPlayerViewModel::class.java,
        )

    override fun getLayoutId() = R.layout.activity_setting_player

    override fun initView() {
        title = "播放器设置"

        dataBinding.viewpager.apply {
            adapter = pageAdapter
            offscreenPageLimit = 2
            currentItem = 0
        }

        TabLayoutMediator(dataBinding.tabLayout, dataBinding.viewpager) { tab, position ->
            tab.text = pageAdapter.getItemTitle(position)
        }.attach()

        tvFocusCoordinator =
            TabLayoutViewPager2DpadFocusCoordinator.attachIfTelevision(
                tabLayout = dataBinding.tabLayout,
                viewPager = dataBinding.viewpager,
                mode = TabDpadMode.SettingsIndicatorOnly,
            )
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN &&
            isTelevisionUiMode() &&
            !dataBinding.tabLayout.isInTouchMode
        ) {
            val toolbar = mToolbar
            val focus = currentFocus
            if (toolbar != null && focus != null && isDescendantOf(focus, toolbar)) {
                if (tvFocusCoordinator?.requestContentFocus() == true) {
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        tvFocusCoordinator?.detach()
        tvFocusCoordinator = null
        super.onDestroy()
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

    inner class SettingFragmentAdapter : FragmentStateAdapter(this@SettingPlayerActivity) {
        private var titles = arrayOf("视频", "弹幕", "字幕")

        override fun getItemCount(): Int = titles.size

        override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> PlayerSettingFragment.newInstance()
                1 -> DanmuSettingFragment.newInstance()
                2 -> SubtitleSettingFragment.newInstance()
                else -> throw IllegalArgumentException()
            }

        fun getItemTitle(position: Int): String = titles.getOrNull(position).orEmpty()
    }
}
